package com.bookapp.ui.book

import io.noties.markwon.Markwon
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.Selection
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.api.AITextRequest
import com.bookapp.data.api.AITextResponse
import com.bookapp.data.api.AIRetrofitClient
import com.bookapp.data.api.EnsureReadingProgressRequest
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.api.UpdateReadingProgressRequest
import com.bookapp.data.model.Chapter
import com.bookapp.data.model.ReadingProgress
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.R as MaterialR
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReaderActivity : AppCompatActivity() {

    private enum class ReadingMode {
        SCROLL, PAGE
    }

    private enum class AiTaskType(
        val titleRes: Int,
        val loadingHintRes: Int
    ) {
        EXPLAIN(
            R.string.reader_ai_title_explain,
            R.string.reader_ai_loading_explain_hint
        ),
        SUMMARIZE(
            R.string.reader_ai_title_summarize,
            R.string.reader_ai_loading_summarize_hint
        )
    }

    private data class FontOption(
        val label: String,
        val family: String
    )

    private data class ReaderAiRequest(
        val task: AiTaskType,
        val selectedText: String,
        val displayText: String,
        val bookName: String,
        val contextBefore: String,
        val contextAfter: String
    )

    private data class AiSheetViews(
        val title: TextView,
        val subtitle: TextView,
        val selectedText: TextView,
        val loadingLayout: View,
        val loadingTitle: TextView,
        val loadingHint: TextView,
        val errorLayout: View,
        val errorMessage: TextView,
        val resultLayout: View,
        val resultContent: TextView,
        val secondaryButton: Button,
        val primaryButton: Button
    )

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
        const val EXTRA_TARGET_CHAPTER_ID = "extra_target_chapter_id"
        private const val MENU_ITEM_AI = 7001
        private const val AI_EXPLAIN_WORD_THRESHOLD = 12
    }

    private lateinit var topBar: View
    private lateinit var bottomPanel: View
    private lateinit var tvBookTitle: TextView
    private lateinit var readerScroll: NestedScrollView
    private lateinit var tvContent: TextView

    private lateinit var tvFooterChapterTitle: TextView
    private lateinit var tvFooterChapterMeta: TextView
    private lateinit var tvFooterProgressPercent: TextView
    private lateinit var tvFooterChapterCounter: TextView
    private lateinit var seekChapterProgress: SeekBar

    private lateinit var btnClose: ImageButton
    private lateinit var btnPrevChapter: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnPlay: ImageButton
    private lateinit var btnChapterList: ImageButton
    private lateinit var btnNextChapter: ImageButton

    private var userId: String? = null
    private var bookId: String? = null
    private var bookTitle: String = "Đọc sách"
    private var targetChapterId: String? = null

    private var chapters: List<Chapter> = emptyList()
    private var currentChapterIndex: Int = 0
    private var controlsVisible: Boolean = true
    private var currentChapterRawText: String = ""
    private var chapterPages: List<String> = emptyList()
    private var currentPageIndex: Int = 0

    private var backgroundColor: Int = 0xFFF1E5C8.toInt()
    private var fontSizeSp: Float = 18f
    private var lineSpacingMultiplier: Float = 1.5f
    private var readingMode: ReadingMode = ReadingMode.SCROLL
    private var fontFamily: String = "sans-serif"
    private var autoScrollSpeed: Int = 20
    private var pageTurnSeconds: Int = 20

    private var isAutoPlaying: Boolean = false
    private val autoPlayHandler = Handler(Looper.getMainLooper())
    private val autoPlayTask = object : Runnable {
        override fun run() {
            if (!isAutoPlaying) return

            if (readingMode == ReadingMode.SCROLL) {
                val child = readerScroll.getChildAt(0)
                val range = if (child == null) 0 else (child.height - readerScroll.height).coerceAtLeast(0)
                if (range <= 0 || readerScroll.scrollY >= range) {
                    stopAutoPlay()
                    return
                }
                // Smooth scrolling: smaller steps with higher frequency for smoother animation
                // Speed range: 8-80, step range: 1-10 pixels per 50ms
                val step = (autoScrollSpeed / 8f).coerceIn(1f, 10f).toInt()
                readerScroll.scrollBy(0, step)
                autoPlayHandler.postDelayed(this, 50L)
            } else {
                val hasNextPage = currentPageIndex < chapterPages.lastIndex
                if (hasNextPage) {
                    currentPageIndex += 1
                    renderCurrentPageOnly()
                    autoPlayHandler.postDelayed(this, (pageTurnSeconds * 1000L).coerceAtLeast(1000L))
                    return
                }

                if (currentChapterIndex < chapters.lastIndex) {
                    switchToChapter(currentChapterIndex + 1)
                    autoPlayHandler.postDelayed(this, (pageTurnSeconds * 1000L).coerceAtLeast(1000L))
                    return
                }

                stopAutoPlay()
            }
        }
    }

    private val fontOptions = listOf(
        FontOption("Roboto (Mặc định)", "sans-serif"),
        FontOption("Roboto Light", "sans-serif-light"),
        FontOption("Roboto Medium", "sans-serif-medium"),
        FontOption("Noto Serif", "serif"),
        FontOption("Roboto Condensed", "sans-serif-condensed"),
        FontOption("Roboto Mono", "monospace"),
        FontOption("Comic (Vui nhộn)", "casual"),
        FontOption("Dancing Script", "cursive"),
    )

    private val prefs by lazy {
        getSharedPreferences("ReaderSettings", MODE_PRIVATE)
    }

    private var downX = 0f
    private var downY = 0f
    private var downEventTime = 0L
    private var dragDetected = false
    private var pageSwipeHandled = false
    private var pageTurnAnimating = false
    private var isSelectionModeActive = false
    private var activeAiCall: Call<AITextResponse>? = null
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private val longPressTimeoutMs by lazy { ViewConfiguration.getLongPressTimeout().toLong() }

    private var isCustomLongPress = false
    private val selectionRunnable = Runnable {
        isCustomLongPress = true
        tvContent.performLongClick()
        isCustomLongPress = false
    }

    private var contentPaddingLeftDefault = 0
    private var contentPaddingTopDefault = 0
    private var contentPaddingRightDefault = 0
    private var contentPaddingBottomDefault = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        bookId = intent.getStringExtra(EXTRA_BOOK_ID)?.trim()?.takeIf { it.isNotEmpty() }
        bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE)?.takeIf { it.isNotBlank() } ?: "Đọc sách"
        targetChapterId = intent.getStringExtra(EXTRA_TARGET_CHAPTER_ID)?.trim()?.takeIf { it.isNotEmpty() }
        userId = getSharedPreferences("BookAppPrefs", MODE_PRIVATE).getString("userId", null)

        if (bookId == null || userId == null) {
            Toast.makeText(this, "Thiếu thông tin để mở trình đọc", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadReaderSettings()
        bindViews()
        applyReaderAppearance()
        setupInteractions()
        loadChaptersAndProgress()
    }

    override fun onPause() {
        super.onPause()
        stopAutoPlay()
        activeAiCall?.cancel()
        persistCurrentProgress()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.readerTopBar)
        bottomPanel = findViewById(R.id.readerBottomPanel)
        tvBookTitle = findViewById(R.id.tvReaderBookTitle)
        readerScroll = findViewById(R.id.readerScroll)
        tvContent = findViewById(R.id.tvReaderContent)

        tvFooterChapterTitle = findViewById(R.id.tvReaderChapterTitle)
        tvFooterChapterMeta = findViewById(R.id.tvReaderChapterMeta)
        tvFooterProgressPercent = findViewById(R.id.tvReaderProgressPercent)
        tvFooterChapterCounter = findViewById(R.id.tvReaderChapterCounter)
        seekChapterProgress = findViewById(R.id.seekReaderProgress)

        btnClose = findViewById(R.id.btnReaderClose)
        btnPrevChapter = findViewById(R.id.btnReaderPrev)
        btnSettings = findViewById(R.id.btnReaderSettings)
        btnPlay = findViewById(R.id.btnReaderPlay)
        btnChapterList = findViewById(R.id.btnReaderList)
        btnNextChapter = findViewById(R.id.btnReaderNext)

        contentPaddingLeftDefault = tvContent.paddingLeft
        contentPaddingTopDefault = tvContent.paddingTop
        contentPaddingRightDefault = tvContent.paddingRight
        contentPaddingBottomDefault = tvContent.paddingBottom

        tvBookTitle.text = bookTitle
        seekChapterProgress.max = 100
        seekChapterProgress.progress = 0
        seekChapterProgress.setOnTouchListener { _, _ -> true }
        tvContent.setTextIsSelectable(true)
        setupSelectionActions()
        updatePlayButtonIcon()
    }

    private fun setupInteractions() {
        btnClose.setOnClickListener {
            persistCurrentProgress()
            finish()
        }

        btnPrevChapter.setOnClickListener {
            if (currentChapterIndex > 0) {
                switchToChapter(currentChapterIndex - 1)
            }
        }

        btnNextChapter.setOnClickListener {
            if (currentChapterIndex < chapters.lastIndex) {
                switchToChapter(currentChapterIndex + 1)
            }
        }

        btnChapterList.setOnClickListener {
            showChapterBottomSheet()
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        btnPlay.setOnClickListener {
            toggleAutoPlay()
        }

        readerScroll.setOnTouchListener { _, event ->
            if (readingMode == ReadingMode.PAGE || isSelectionModeActive) {
                return@setOnTouchListener false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopAutoPlay()
                    downX = event.x
                    downY = event.y
                    dragDetected = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragDetected) {
                        val dx = abs(event.x - downX)
                        val dy = abs(event.y - downY)
                        if (dx > touchSlop || dy > touchSlop) {
                            dragDetected = true
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragDetected) {
                        hideControls()
                    } else {
                        showControlsAndRefresh()
                    }
                }
            }
            false
        }

        tvContent.setOnTouchListener { _, event ->
            // Always track DOWN/MOVE/UP for the 3-second custom selection delay
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    downEventTime = event.eventTime
                    
                    tvContent.removeCallbacks(selectionRunnable)
                    tvContent.postDelayed(selectionRunnable, 1000)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)
                    if (dx > touchSlop || dy > touchSlop) {
                        tvContent.removeCallbacks(selectionRunnable)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    tvContent.removeCallbacks(selectionRunnable)
                }
            }

            if (readingMode != ReadingMode.PAGE) {
                return@setOnTouchListener false
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopAutoPlay()
                    pageSwipeHandled = false
                    false
                }

                MotionEvent.ACTION_MOVE -> false

                MotionEvent.ACTION_CANCEL -> false

                MotionEvent.ACTION_UP -> {
                    if (isSelectionModeActive || pageTurnAnimating || pageSwipeHandled || hasValidTextSelection()) {
                        return@setOnTouchListener false
                    }

                    val pressDuration = (event.eventTime - downEventTime).coerceAtLeast(0L)
                    if (pressDuration >= longPressTimeoutMs) {
                        return@setOnTouchListener false
                    }

                    val dx = event.x - downX
                    val dy = event.y - downY
                    val moved = abs(dx) > touchSlop || abs(dy) > touchSlop
                    
                    // Fix: Removed 'fromMiddle' restriction. Users often start swiping from
                    // the edges of the screen, which caused 'fromMiddle' to be false and breaking the swipe.
                    val isHorizontalSwipe = abs(dx) > (touchSlop * 2) && abs(dx) > abs(dy)

                    if (isHorizontalSwipe) {
                        pageSwipeHandled = true
                        if (dx < 0) {
                            moveToNextPageOrChapterWithAnimation()
                        } else {
                            moveToPrevPageOrChapterWithAnimation()
                        }
                        if (moved && controlsVisible) {
                            hideControls()
                        }
                        return@setOnTouchListener true
                    }

                    if (moved) {
                        if (controlsVisible) {
                            hideControls()
                        }
                        return@setOnTouchListener true
                    }

                    // Taps without movement: allow intuitive edge-tapping
                    if (!controlsVisible) {
                        val widthOffset = tvContent.width
                        when {
                            event.x > widthOffset * 0.75f -> {
                                moveToNextPageOrChapterWithAnimation()
                                return@setOnTouchListener true
                            }
                            event.x < widthOffset * 0.25f -> {
                                moveToPrevPageOrChapterWithAnimation()
                                return@setOnTouchListener true
                            }
                        }
                    }

                    // Tap in the middle area or close controls
                    if (controlsVisible) {
                        hideControls()
                    } else {
                        showControlsAndRefresh()
                    }
                    true
                }

                else -> false
            }
        }

        tvContent.setOnLongClickListener {
            stopAutoPlay()
            if (!isCustomLongPress) {
                // Blocks the default 500ms long press so it doesn't trigger selection early
                return@setOnLongClickListener true
            }
            // Allow the 3000ms custom long press
            false
        }
    }

    private fun hasValidTextSelection(): Boolean {
        val text = tvContent.text ?: return false
        val start = minOf(tvContent.selectionStart, tvContent.selectionEnd)
        val end = maxOf(tvContent.selectionStart, tvContent.selectionEnd)
        return start >= 0 && end > start && end <= text.length
    }

    private fun loadChaptersAndProgress() {
        val safeBookId = bookId ?: return
        RetrofitClient.instance.getChaptersByBook(safeBookId)
            .enqueue(object : Callback<List<Chapter>> {
                override fun onResponse(call: Call<List<Chapter>>, response: Response<List<Chapter>>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@ReaderActivity, "Không tải được danh sách chương", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    val loaded = response.body().orEmpty().filter { !it.id.isNullOrBlank() }
                    if (loaded.isEmpty()) {
                        Toast.makeText(this@ReaderActivity, "Sách chưa có chương", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                    chapters = loaded
                    ensureReadingProgress()
                }

                override fun onFailure(call: Call<List<Chapter>>, t: Throwable) {
                    Toast.makeText(this@ReaderActivity, "Lỗi tải chương: ${t.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun ensureReadingProgress() {
        val safeUserId = userId ?: return
        val safeBookId = bookId ?: return

        RetrofitClient.instance.ensureReadingProgress(
            EnsureReadingProgressRequest(safeUserId, safeBookId)
        ).enqueue(object : Callback<ReadingProgress> {
            override fun onResponse(call: Call<ReadingProgress>, response: Response<ReadingProgress>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@ReaderActivity, "Không tạo được tiến trình đọc", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val progress = response.body()
                val requestedChapter = targetChapterId
                if (!requestedChapter.isNullOrBlank()) {
                    val requestedIndex = chapters.indexOfFirst { it.id == requestedChapter }
                    if (requestedIndex >= 0) {
                        targetChapterId = null
                        switchToChapter(requestedIndex)
                        return
                    }
                }

                val targetChapterId = progress?.chapterId
                val targetIndex = chapters.indexOfFirst { it.id == targetChapterId }.takeIf { it >= 0 } ?: 0
                currentChapterIndex = targetIndex
                val percent = (progress?.chapterProgressPercent ?: 0).coerceIn(0, 100)
                showChapter(targetIndex, percent)
            }

            override fun onFailure(call: Call<ReadingProgress>, t: Throwable) {
                Toast.makeText(this@ReaderActivity, "Lỗi tiến trình: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun showChapter(index: Int, initialPercent: Int) {
        if (chapters.isEmpty() || index !in chapters.indices) return

        // BUG-3 FIX: Always reset selection mode state when switching chapters.
        // Auto-play or chapter navigation can switch chapters while action mode is active,
        // leaving isSelectionModeActive=true which breaks SCROLL touch handling.
        isSelectionModeActive = false
        dragDetected = false
        clearTextSelection()
        currentChapterIndex = index
        val chapter = chapters[index]
        val rawContent = chapter.content?.takeIf { it.isNotBlank() } ?: "(Chương này chưa có nội dung)"
        currentChapterRawText = processContentMergeLonelyChars(rawContent)

        val chapterTitle = chapter.title?.takeIf { it.isNotBlank() } ?: "(Không có tiêu đề)"
        tvFooterChapterTitle.text = chapterTitle
        tvFooterChapterCounter.text = "${index + 1}/${chapters.size}"

        btnPrevChapter.isEnabled = index > 0
        btnNextChapter.isEnabled = index < chapters.lastIndex
        btnPrevChapter.alpha = if (btnPrevChapter.isEnabled) 1f else 0.35f
        btnNextChapter.alpha = if (btnNextChapter.isEnabled) 1f else 0.35f

        if (readingMode == ReadingMode.PAGE) {
            renderPageModeChapter(initialPercent)
            showControlsAndRefresh()
        } else {
            tvContent.text = currentChapterRawText
            readerScroll.post {
                val child = readerScroll.getChildAt(0)
                if (child != null) {
                    val range = (child.height - readerScroll.height).coerceAtLeast(0)
                    val targetY = if (range <= 0) 0 else ((range * (initialPercent / 100f))).roundToInt()
                    readerScroll.scrollTo(0, targetY)
                } else {
                    readerScroll.scrollTo(0, 0)
                }
                tvFooterChapterMeta.text = ""
                showControlsAndRefresh()
            }
        }
    }

    private fun switchToChapter(targetIndex: Int) {
        if (targetIndex !in chapters.indices) return

        stopAutoPlay()
        currentChapterIndex = targetIndex
        val chapterId = chapters[targetIndex].id ?: return

        updateReadingProgress(chapterId, 0, onSuccess = {
            showChapter(targetIndex, 0)
        })
    }

    private fun showChapterBottomSheet() {
        stopAutoPlay()
        val dialog = BottomSheetDialog(this)
        val content = layoutInflater.inflate(R.layout.dialog_reader_chapters, null)
        dialog.setContentView(content)

        val pageSize = 15
        content.findViewById<TextView>(R.id.tvDialogChapterTitle)?.text = bookTitle
        content.findViewById<ImageButton>(R.id.btnDialogChapterClose)?.setOnClickListener {
            dialog.dismiss()
        }

        val recycler = content.findViewById<RecyclerView>(R.id.recyclerReaderChapters)
        val tvPageIndicator = content.findViewById<TextView>(R.id.tvPageIndicator)
        val btnPageFirst = content.findViewById<ImageButton>(R.id.btnPageFirst)
        val btnPagePrev = content.findViewById<ImageButton>(R.id.btnPagePrev)
        val btnPageNext = content.findViewById<ImageButton>(R.id.btnPageNext)
        val btnPageLast = content.findViewById<ImageButton>(R.id.btnPageLast)

        val adapter = ReaderChapterAdapter { selectedChapter ->
            dialog.dismiss()
            val selectedIndex = chapters.indexOfFirst { it.id == selectedChapter.id }
            if (selectedIndex >= 0 && selectedIndex != currentChapterIndex) {
                switchToChapter(selectedIndex)
            }
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val totalPages = ((chapters.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        var currentPage = (currentChapterIndex / pageSize).coerceIn(0, totalPages - 1)

        fun updatePagerButtons() {
            val canGoPrev = currentPage > 0
            val canGoNext = currentPage < totalPages - 1

            btnPageFirst.isEnabled = canGoPrev
            btnPagePrev.isEnabled = canGoPrev
            btnPageNext.isEnabled = canGoNext
            btnPageLast.isEnabled = canGoNext

            btnPageFirst.alpha = if (canGoPrev) 1f else 0.35f
            btnPagePrev.alpha = if (canGoPrev) 1f else 0.35f
            btnPageNext.alpha = if (canGoNext) 1f else 0.35f
            btnPageLast.alpha = if (canGoNext) 1f else 0.35f
        }

        fun renderPage() {
            val start = currentPage * pageSize
            val end = (start + pageSize).coerceAtMost(chapters.size)
            val pageItems = chapters.subList(start, end)
            val selectedChapterId = chapters.getOrNull(currentChapterIndex)?.id

            adapter.submitList(pageItems, selectedChapterId)
            tvPageIndicator.text = "${currentPage + 1}/$totalPages"
            updatePagerButtons()
        }

        btnPageFirst.setOnClickListener {
            if (currentPage != 0) {
                currentPage = 0
                renderPage()
            }
        }

        btnPagePrev.setOnClickListener {
            if (currentPage > 0) {
                currentPage -= 1
                renderPage()
            }
        }

        btnPageNext.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage += 1
                renderPage()
            }
        }

        btnPageLast.setOnClickListener {
            if (currentPage != totalPages - 1) {
                currentPage = totalPages - 1
                renderPage()
            }
        }

        renderPage()

        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.show()
        dialog.findViewById<View>(MaterialR.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    private fun showSettingsDialog() {
        stopAutoPlay()
        val dialog = BottomSheetDialog(this)
        val content = layoutInflater.inflate(R.layout.dialog_reader_settings, null)
        dialog.setContentView(content)

        content.findViewById<ImageButton>(R.id.btnSettingClose)?.setOnClickListener {
            dialog.dismiss()
        }

        val tvFontSize = content.findViewById<TextView>(R.id.tvSettingFontSizeValue)
        val tvLineSpacing = content.findViewById<TextView>(R.id.tvSettingLineSpacingValue)
        val tvAutoSpeed = content.findViewById<TextView>(R.id.tvSettingAutoSpeedValue)
        val tvPageTurn = content.findViewById<TextView>(R.id.tvSettingPageTurnValue)
        val spinnerFont = content.findViewById<Spinner>(R.id.spinnerSettingFont)

        val btnFontMinus = content.findViewById<ImageButton>(R.id.btnSettingFontMinus)
        val btnFontPlus = content.findViewById<ImageButton>(R.id.btnSettingFontPlus)
        val btnLineMinus = content.findViewById<ImageButton>(R.id.btnSettingLineMinus)
        val btnLinePlus = content.findViewById<ImageButton>(R.id.btnSettingLinePlus)
        val btnSpeedMinus = content.findViewById<ImageButton>(R.id.btnSettingSpeedMinus)
        val btnSpeedPlus = content.findViewById<ImageButton>(R.id.btnSettingSpeedPlus)
        val btnPageTurnMinus = content.findViewById<ImageButton>(R.id.btnSettingPageTurnMinus)
        val btnPageTurnPlus = content.findViewById<ImageButton>(R.id.btnSettingPageTurnPlus)

        val btnModeScroll = content.findViewById<TextView>(R.id.btnModeScroll)
        val btnModePage = content.findViewById<TextView>(R.id.btnModePage)

        val colorAuto = content.findViewById<TextView>(R.id.colorAuto)
        val colorWhite = content.findViewById<View>(R.id.colorWhite)
        val colorBlack = content.findViewById<View>(R.id.colorBlack)
        val colorCream = content.findViewById<View>(R.id.colorCream)
        val colorGray = content.findViewById<View>(R.id.colorGray)

        val fontAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            fontOptions.map { it.label }
        )
        spinnerFont.adapter = fontAdapter
        val currentFontIndex = fontOptions.indexOfFirst { it.family == fontFamily }.takeIf { it >= 0 } ?: 0
        spinnerFont.setSelection(currentFontIndex)

        fun refreshSettingViews() {
            tvFontSize.text = String.format("%.0f", fontSizeSp)
            tvLineSpacing.text = String.format("%.1f", lineSpacingMultiplier)
            tvAutoSpeed.text = autoScrollSpeed.toString()
            tvPageTurn.text = pageTurnSeconds.toString()

            styleModeButton(btnModeScroll, readingMode == ReadingMode.SCROLL)
            styleModeButton(btnModePage, readingMode == ReadingMode.PAGE)

            // BUG-9 FIX: Auto and Cream previously shared the same hex value 0xFFF1E5C8,
            // causing both to appear selected simultaneously. "Auto" is now the warm cream
            // preset (0xFFF1E5C8) while the manual "Cream" swatch uses a slightly richer
            // 0xFFEDD9AA so the two can be independently highlighted.
            styleColorChip(colorAuto, backgroundColor == 0xFFF1E5C8.toInt())
            styleColorBlock(colorWhite, 0xFFFFFFFF.toInt(), backgroundColor == 0xFFFFFFFF.toInt())
            styleColorBlock(colorBlack, 0xFF1A1A1A.toInt(), backgroundColor == 0xFF1A1A1A.toInt())
            styleColorBlock(colorCream, 0xFFEDD9AA.toInt(), backgroundColor == 0xFFEDD9AA.toInt())
            styleColorBlock(colorGray, 0xFFE9EDF2.toInt(), backgroundColor == 0xFFE9EDF2.toInt())
        }

        btnFontMinus.setOnClickListener {
            fontSizeSp = (fontSizeSp - 1f).coerceAtLeast(14f)
            applyReaderAppearance()
            refreshSettingViews()
        }
        btnFontPlus.setOnClickListener {
            fontSizeSp = (fontSizeSp + 1f).coerceAtMost(34f)
            applyReaderAppearance()
            refreshSettingViews()
        }

        btnLineMinus.setOnClickListener {
            lineSpacingMultiplier = (lineSpacingMultiplier - 0.1f).coerceAtLeast(1.1f)
            applyReaderAppearance()
            refreshSettingViews()
        }
        btnLinePlus.setOnClickListener {
            lineSpacingMultiplier = (lineSpacingMultiplier + 0.1f).coerceAtMost(2.4f)
            applyReaderAppearance()
            refreshSettingViews()
        }

        btnSpeedMinus.setOnClickListener {
            autoScrollSpeed = (autoScrollSpeed - 2).coerceAtLeast(8)
            refreshSettingViews()
        }
        btnSpeedPlus.setOnClickListener {
            autoScrollSpeed = (autoScrollSpeed + 2).coerceAtMost(80)
            refreshSettingViews()
        }

        btnPageTurnMinus.setOnClickListener {
            pageTurnSeconds = (pageTurnSeconds - 1).coerceAtLeast(3)
            refreshSettingViews()
        }
        btnPageTurnPlus.setOnClickListener {
            pageTurnSeconds = (pageTurnSeconds + 1).coerceAtMost(120)
            refreshSettingViews()
        }

        btnModeScroll.setOnClickListener {
            if (readingMode != ReadingMode.SCROLL) {
                val currentPercent = calculateCurrentProgressPercent()
                readingMode = ReadingMode.SCROLL
                applyReaderAppearance(currentPercent)
            }
            refreshSettingViews()
        }
        btnModePage.setOnClickListener {
            if (readingMode != ReadingMode.PAGE) {
                val currentPercent = calculateCurrentProgressPercent()
                readingMode = ReadingMode.PAGE
                applyReaderAppearance(currentPercent)
            }
            refreshSettingViews()
        }

        colorAuto.setOnClickListener {
            backgroundColor = 0xFFF1E5C8.toInt()
            applyReaderAppearance()
            refreshSettingViews()
        }
        colorWhite.setOnClickListener {
            backgroundColor = 0xFFFFFFFF.toInt()
            applyReaderAppearance()
            refreshSettingViews()
        }
        colorBlack.setOnClickListener {
            backgroundColor = 0xFF1A1A1A.toInt()
            applyReaderAppearance()
            refreshSettingViews()
        }
        colorCream.setOnClickListener {
            // BUG-9 FIX: Use distinct color so it doesn't collide with 'Auto'
            backgroundColor = 0xFFEDD9AA.toInt()
            applyReaderAppearance()
            refreshSettingViews()
        }
        colorGray.setOnClickListener {
            backgroundColor = 0xFFE9EDF2.toInt()
            applyReaderAppearance()
            refreshSettingViews()
        }

        spinnerFont.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = fontOptions.getOrNull(position) ?: return
                if (selected.family != fontFamily) {
                    fontFamily = selected.family
                    applyReaderAppearance()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        })

        refreshSettingViews()

        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.setOnDismissListener {
            saveReaderSettings()
        }
        dialog.show()
        dialog.findViewById<View>(MaterialR.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    private fun showControlsAndRefresh() {
        controlsVisible = true
        topBar.isVisible = true
        bottomPanel.isVisible = true
        refreshProgressUi(calculateCurrentProgressPercent())
    }

    private fun hideControls() {
        controlsVisible = false
        topBar.isVisible = false
        bottomPanel.isVisible = false
    }

    private fun refreshProgressUi(percent: Int) {
        val safePercent = percent.coerceIn(0, 100)
        seekChapterProgress.progress = safePercent
        tvFooterProgressPercent.text = "$safePercent%"
        if (readingMode == ReadingMode.PAGE) {
            tvFooterChapterMeta.text = "Trang ${currentPageIndex + 1}/${chapterPages.size.coerceAtLeast(1)}"
        } else {
            tvFooterChapterMeta.text = ""
        }
    }

    private fun calculateCurrentProgressPercent(): Int {
        if (readingMode == ReadingMode.PAGE) {
            if (chapterPages.isEmpty()) return 0
            // BUG-8 FIX: Single-page chapter should show 0% at start and 100% only when
            // it's the last chapter. Returning 100 immediately confused users who just opened
            // a short chapter and saw the progress bar already full.
            if (chapterPages.size == 1) return if (currentPageIndex == 0) 0 else 100
            val ratio = currentPageIndex.toFloat() / chapterPages.lastIndex.toFloat()
            return (ratio * 100f).roundToInt().coerceIn(0, 100)
        }

        val child = readerScroll.getChildAt(0) ?: return 0
        val range = child.height - readerScroll.height
        if (range <= 0) return 100

        val ratio = readerScroll.scrollY.toFloat() / range.toFloat()
        return (ratio * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun persistCurrentProgress() {
        if (chapters.isEmpty()) return
        val chapterId = chapters[currentChapterIndex].id ?: return
        val percent = calculateCurrentProgressPercent()
        updateReadingProgress(chapterId, percent)
    }

    private fun updateReadingProgress(
        chapterId: String,
        percent: Int,
        onSuccess: (() -> Unit)? = null
    ) {
        val safeUserId = userId ?: return
        val safeBookId = bookId ?: return

        RetrofitClient.instance.updateReadingProgress(
            UpdateReadingProgressRequest(
                userId = safeUserId,
                bookId = safeBookId,
                chapterId = chapterId,
                chapterProgressPercent = percent.coerceIn(0, 100)
            )
        ).enqueue(object : Callback<ReadingProgress> {
            override fun onResponse(call: Call<ReadingProgress>, response: Response<ReadingProgress>) {
                if (response.isSuccessful) {
                    onSuccess?.invoke()
                }
            }

            override fun onFailure(call: Call<ReadingProgress>, t: Throwable) {
            }
        })
    }

    private fun renderCurrentPageOnly() {
        if (chapterPages.isEmpty()) {
            clearTextSelection()
            tvContent.text = currentChapterRawText
            return
        }
        currentPageIndex = currentPageIndex.coerceIn(0, chapterPages.lastIndex)
        clearTextSelection()
        tvContent.text = chapterPages[currentPageIndex]
        readerScroll.scrollTo(0, 0)
        refreshProgressUi(calculateCurrentProgressPercent())
    }

    private fun animatePageTurn(direction: Int, onSwitch: () -> Unit) {
        if (pageTurnAnimating) return
        pageTurnAnimating = true

        val width = tvContent.width.toFloat().coerceAtLeast(1f)
        val outX = if (direction < 0) -width * 0.20f else width * 0.20f
        val inX = -outX

        tvContent.animate()
            .translationX(outX)
            .alpha(0.10f)
            .setDuration(120L)
            .withEndAction {
                onSwitch()
                tvContent.translationX = inX
                tvContent.alpha = 0.10f
                tvContent.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(150L)
                    .withEndAction {
                        pageTurnAnimating = false
                    }
                    .start()
            }
            .start()
    }

    private fun moveToNextPageOrChapterWithAnimation() {
        if (readingMode != ReadingMode.PAGE) return

        if (currentPageIndex < chapterPages.lastIndex) {
            animatePageTurn(direction = -1) {
                currentPageIndex += 1
                renderCurrentPageOnly()
            }
            return
        }

        if (currentChapterIndex < chapters.lastIndex) {
            switchToChapter(currentChapterIndex + 1)
        }
    }

    private fun moveToPrevPageOrChapterWithAnimation() {
        if (readingMode != ReadingMode.PAGE) return

        if (currentPageIndex > 0) {
            animatePageTurn(direction = 1) {
                currentPageIndex -= 1
                renderCurrentPageOnly()
            }
            return
        }

        if (currentChapterIndex > 0) {
            // BUG-2 FIX: Pass initialPercent=100 so that renderPageModeChapter() will
            // position us at the LAST page of the previous chapter. Previously we used
            // readerScroll.post{ currentPageIndex = chapterPages.lastIndex } which ran
            // before the network call finished, reading stale chapterPages data.
            stopAutoPlay()
            currentChapterIndex -= 1
            val chapterId = chapters[currentChapterIndex].id ?: return
            updateReadingProgress(chapterId, 100, onSuccess = {
                showChapter(currentChapterIndex, 100)
            })
        }
    }

    private fun paginateChapter(rawText: String): List<String> {
        if (rawText.isBlank()) return listOf("(Chương này chưa có nội dung)")

        val density = resources.displayMetrics.density
        val fallbackWidth = (resources.displayMetrics.widthPixels - (48f * density)).toInt()
            .coerceAtLeast((220f * density).toInt())
        val availableWidth = (tvContent.width - tvContent.paddingLeft - tvContent.paddingRight)
            .takeIf { it > 0 }
            ?: fallbackWidth

        val fallbackHeight = (resources.displayMetrics.heightPixels * 0.82f).toInt()
            .coerceAtLeast((260f * density).toInt())
        val availableHeight = (readerScroll.height - tvContent.paddingTop - tvContent.paddingBottom)
            .takeIf { it > 0 }
            ?: fallbackHeight

        val textPaint = TextPaint(tvContent.paint).apply {
            textSize = fontSizeSp * resources.displayMetrics.scaledDensity
            typeface = Typeface.create(fontFamily, Typeface.NORMAL)
        }

        fun buildLayout(text: String): StaticLayout {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                StaticLayout.Builder
                    .obtain(text, 0, text.length, textPaint, availableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, lineSpacingMultiplier)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(
                    text,
                    textPaint,
                    availableWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    lineSpacingMultiplier,
                    0f,
                    false
                )
            }
        }

        val pages = mutableListOf<String>()
        var cursor = 0

        while (cursor < rawText.length) {
            val remaining = rawText.substring(cursor)

            var low = 1
            var high = remaining.length
            var best = 1

            while (low <= high) {
                val mid = (low + high) ushr 1
                val candidate = remaining.substring(0, mid)
                val height = buildLayout(candidate).height
                if (height <= availableHeight) {
                    best = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }

            var endOffset = best.coerceAtLeast(1)
            if (endOffset < remaining.length) {
                val breakAt = remaining.lastIndexOfAny(charArrayOf(' ', '\n', '\t'), endOffset)
                if (breakAt >= (endOffset * 0.6f).toInt()) {
                    endOffset = breakAt
                }
            }

            val pageText = remaining.substring(0, endOffset).trim()
            if (pageText.isNotEmpty()) {
                pages.add(pageText)
            }

            cursor += endOffset
            while (cursor < rawText.length && rawText[cursor].isWhitespace()) {
                cursor += 1
            }
        }

        return pages.ifEmpty { listOf(rawText.trim()) }
    }

    private fun renderPageModeChapter(initialPercent: Int) {
        fun applyPage(percent: Int) {
            chapterPages = paginateChapter(currentChapterRawText)
            val safeLast = chapterPages.lastIndex.coerceAtLeast(0)
            currentPageIndex = ((percent / 100f) * safeLast).roundToInt().coerceIn(0, safeLast)
            renderCurrentPageOnly()
        }

        if (readerScroll.height > 0 && tvContent.width > 0) {
            applyPage(initialPercent)
        } else {
            readerScroll.post {
                applyPage(initialPercent)
            }
        }
    }

    private fun toggleAutoPlay() {
        if (isAutoPlaying) {
            stopAutoPlay()
        } else {
            startAutoPlay()
        }
    }

    private fun startAutoPlay() {
        if (chapters.isEmpty()) return
        if (controlsVisible) {
            hideControls()
        }
        isAutoPlaying = true
        updatePlayButtonIcon()
        autoPlayHandler.removeCallbacks(autoPlayTask)
        autoPlayHandler.post(autoPlayTask)
    }

    private fun stopAutoPlay() {
        if (!isAutoPlaying) return
        isAutoPlaying = false
        autoPlayHandler.removeCallbacks(autoPlayTask)
        updatePlayButtonIcon()
    }

    private fun updatePlayButtonIcon() {
        btnPlay.setImageResource(
            if (isAutoPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun loadReaderSettings() {
        backgroundColor = prefs.getInt("backgroundColor", 0xFFF1E5C8.toInt())
        fontSizeSp = prefs.getFloat("fontSizeSp", 18f)
        lineSpacingMultiplier = prefs.getFloat("lineSpacingMultiplier", 1.5f)
        readingMode = if (prefs.getString("readingMode", "SCROLL") == "PAGE") {
            ReadingMode.PAGE
        } else {
            ReadingMode.SCROLL
        }
        fontFamily = prefs.getString("fontFamily", "sans-serif") ?: "sans-serif"
        autoScrollSpeed = prefs.getInt("autoScrollSpeed", 20)
        pageTurnSeconds = prefs.getInt("pageTurnSeconds", 20)
    }

    private fun saveReaderSettings() {
        prefs.edit()
            .putInt("backgroundColor", backgroundColor)
            .putFloat("fontSizeSp", fontSizeSp)
            .putFloat("lineSpacingMultiplier", lineSpacingMultiplier)
            .putString("readingMode", if (readingMode == ReadingMode.PAGE) "PAGE" else "SCROLL")
            .putString("fontFamily", fontFamily)
            .putInt("autoScrollSpeed", autoScrollSpeed)
                .putInt("pageTurnSeconds", pageTurnSeconds)
            .apply()
    }

    private fun applyReaderAppearance(percentHint: Int? = null) {
        findViewById<View>(R.id.readerRoot).setBackgroundColor(backgroundColor)
        readerScroll.setBackgroundColor(backgroundColor)
        tvContent.setBackgroundColor(backgroundColor)

        tvContent.textSize = fontSizeSp
        tvContent.setLineSpacing(0f, lineSpacingMultiplier)
        tvContent.typeface = Typeface.create(fontFamily, Typeface.NORMAL)

        val darkBackground = ColorUtils.calculateLuminance(backgroundColor) < 0.5
        tvContent.setTextColor(if (darkBackground) 0xFFECECEC.toInt() else 0xFF121212.toInt())

        if (readingMode == ReadingMode.PAGE) {
            val density = resources.displayMetrics.density
            val horizontalPadding = (24f * density).roundToInt()
            val verticalPadding = (16f * density).roundToInt()
            tvContent.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            readerScroll.isNestedScrollingEnabled = false
            renderPageModeChapter(percentHint ?: calculateCurrentProgressPercent())
        } else {
            tvContent.setPadding(
                contentPaddingLeftDefault,
                contentPaddingTopDefault,
                contentPaddingRightDefault,
                contentPaddingBottomDefault
            )
            readerScroll.isNestedScrollingEnabled = true
            clearTextSelection()
            tvContent.text = currentChapterRawText
            val percent = percentHint ?: calculateCurrentProgressPercent()
            readerScroll.post {
                val child = readerScroll.getChildAt(0)
                val range = if (child == null) 0 else (child.height - readerScroll.height).coerceAtLeast(0)
                val targetY = if (range <= 0) 0 else ((range * (percent / 100f))).roundToInt()
                readerScroll.scrollTo(0, targetY)
                refreshProgressUi(calculateCurrentProgressPercent())
            }
        }
    }

    private fun styleModeButton(target: TextView, selected: Boolean) {
        target.setBackgroundResource(if (selected) R.drawable.reader_btn_primary_bg else R.drawable.reader_btn_outline_bg)
        target.setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFF944A00.toInt())
    }

    private fun styleColorChip(target: TextView, selected: Boolean) {
        target.setBackgroundResource(if (selected) R.drawable.reader_btn_primary_bg else R.drawable.reader_btn_outline_bg)
        target.setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFF944A00.toInt())
    }

    private fun styleColorBlock(target: View, color: Int, selected: Boolean) {
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 8f * resources.displayMetrics.density
            setStroke(
                if (selected) (3f * resources.displayMetrics.density).roundToInt() else 1,
                if (selected) 0xFF944A00.toInt() else 0xFF9FA5AE.toInt()
            )
        }
        target.background = drawable
    }

    private fun setupSelectionActions() {
        tvContent.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                if (menu == null) return false
                isSelectionModeActive = true
                stopAutoPlay()
                addAiSelectionMenuItem(menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                // BUG-1 FIX: Do NOT add menu item here — onPrepareActionMode is called
                // repeatedly (e.g. when selection changes) and can cause duplicate items
                // on certain OEM variants. Menu is already populated in onCreateActionMode.
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (item?.itemId != MENU_ITEM_AI) return false

                val request = buildAiRequestFromSelection()
                if (request == null) {
                    Toast.makeText(
                        this@ReaderActivity,
                        R.string.reader_ai_no_selection,
                        Toast.LENGTH_SHORT
                    ).show()
                    mode?.finish()
                    return true
                }

                mode?.finish()
                showAiResultBottomSheet(request)
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                isSelectionModeActive = false
                // BUG-7 FIX: Reset drag state so the next tap after closing selection
                // does not mistakenly trigger hideControls() due to stale dragDetected=true
                dragDetected = false
                clearTextSelection()
            }
        }
    }

    private fun addAiSelectionMenuItem(menu: Menu) {
        val existing = menu.findItem(MENU_ITEM_AI)
        if (existing != null) return

        // Create a beautifully decorated, eye-catching text for the AI menu item
        // because typical text selection toolbars can be boring or ignore icons.
        val titleText = "✨ Phân tích AI"
        val spannableTitle = android.text.SpannableString(titleText)
        
        // Make it bold
        spannableTitle.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0, titleText.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Add a striking purple/blue AI-themed color
        spannableTitle.setSpan(
            android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#6200EA")),
            0, titleText.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val item = menu.add(Menu.NONE, MENU_ITEM_AI, 0, spannableTitle)
        
        // Still provide the Drawable icon for OEMs that do render icons in ActionMode
        item.icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.ic_ai_spark)
        
        // Force it to always show up directly on the bar so the user doesn't have to open the overflow (...) menu
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
    }

    private fun buildAiRequestFromSelection(): ReaderAiRequest? {
        val text = tvContent.text ?: return null
        val start = minOf(tvContent.selectionStart, tvContent.selectionEnd)
        val end = maxOf(tvContent.selectionStart, tvContent.selectionEnd)
        if (start < 0 || end <= start || end > text.length) return null

        val rawSelection = text.subSequence(start, end).toString().trim()
        val displayText = rawSelection
            .replace(Regex("[\\t\\x0B\\f\\r]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        val compactText = rawSelection.replace(Regex("\\s+"), " ").trim()
        if (compactText.isBlank()) return null

        val wordCount = compactText.split(Regex("\\s+")).count { it.isNotBlank() }
        val task = if (wordCount <= AI_EXPLAIN_WORD_THRESHOLD) {
            AiTaskType.EXPLAIN
        } else {
            AiTaskType.SUMMARIZE
        }

        val safeBookName = tvBookTitle.text?.toString()?.trim().orEmpty().ifEmpty { bookTitle }

        val contextBeforeStart = maxOf(0, start - 300)
        val contextBefore = text.subSequence(contextBeforeStart, start).toString().trim()
        
        val contextAfterEnd = minOf(text.length, end + 300)
        val contextAfter = text.subSequence(end, contextAfterEnd).toString().trim()

        return ReaderAiRequest(
            task = task,
            selectedText = compactText,
            displayText = displayText,
            bookName = safeBookName,
            contextBefore = contextBefore,
            contextAfter = contextAfter
        )
    }

    private fun clearTextSelection() {
        val current = tvContent.text
        if (current is Spannable) {
            Selection.removeSelection(current)
        }
    }

    private fun processContentMergeLonelyChars(content: String): String {
        val lines = content.lines()
        if (lines.isEmpty()) return content

        val processed = mutableListOf<String>()
        val checkLimit = 10.coerceAtMost(lines.size)
        var i = 0

        // Xử lý 10 hàng đầu tiên
        while (i < checkLimit) {
            val line = lines[i].trim()
            
            // Kiểm tra nếu hàng chỉ có 1 ký tự alphabet đứng lẻ
            if (line.length == 1 && line[0].isLetter()) {
                // Nếu còn hàng tiếp theo, gộp ký tự này vào hàng sau (không có dấu cách)
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    processed.add("$line$nextLine")
                    i += 2 // Bỏ qua hàng tiếp theo vì đã gộp rồi
                } else {
                    // Nếu đây là hàng cuối cùng, giữ nguyên
                    processed.add(lines[i])
                    i++
                }
            } else {
                processed.add(lines[i])
                i++
            }
        }

        // Giữ nguyên các hàng còn lại (sau hàng 10)
        if (i < lines.size) {
            processed.addAll(lines.subList(i, lines.size))
        }

        return processed.joinToString("\n")
    }

    private fun showAiResultBottomSheet(request: ReaderAiRequest) {
        stopAutoPlay()
        val dialog = BottomSheetDialog(this)
        val content = layoutInflater.inflate(R.layout.dialog_reader_ai_result, null)
        dialog.setContentView(content)

        val views = AiSheetViews(
            title = content.findViewById(R.id.tvAiSheetTitle),
            subtitle = content.findViewById(R.id.tvAiSheetSubtitle),
            selectedText = content.findViewById(R.id.tvAiSelectedText),
            loadingLayout = content.findViewById(R.id.layoutAiLoading),
            loadingTitle = content.findViewById(R.id.tvAiLoadingTitle),
            loadingHint = content.findViewById(R.id.tvAiLoadingHint),
            errorLayout = content.findViewById(R.id.layoutAiError),
            errorMessage = content.findViewById(R.id.tvAiErrorMessage),
            resultLayout = content.findViewById(R.id.layoutAiResult),
            resultContent = content.findViewById(R.id.tvAiResultContent),
            secondaryButton = content.findViewById(R.id.btnAiSecondary),
            primaryButton = content.findViewById(R.id.btnAiPrimary)
        )

        views.title.setText(request.task.titleRes)
        views.subtitle.setText(R.string.reader_ai_sheet_subtitle)
        views.selectedText.text = request.displayText

        content.findViewById<ImageButton>(R.id.btnAiSheetClose)?.setOnClickListener {
            dialog.dismiss()
        }

        views.secondaryButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.setOnDismissListener {
            activeAiCall?.cancel()
            activeAiCall = null
        }
        // BUG-11 FIX: When user long-presses to copy the AI result text, the soft keyboard
        // may appear and cause the BottomSheetDialog to shrink/collapse. Setting
        // SOFT_INPUT_ADJUST_RESIZE tells the window to resize its content area instead of
        // pushing the sheet upward, keeping the sheet fully expanded.
        dialog.window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        dialog.show()
        dialog.findViewById<View>(MaterialR.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)

        executeAiRequest(dialog, views, request)
    }

    private fun executeAiRequest(
        dialog: BottomSheetDialog,
        views: AiSheetViews,
        request: ReaderAiRequest
    ) {
        renderAiLoadingState(views, request)

        activeAiCall?.cancel()
        val call = when (request.task) {
            AiTaskType.EXPLAIN -> AIRetrofitClient.instance.explain(
                AITextRequest(
                    text = request.selectedText,
                    book_name = request.bookName,
                    context_before = request.contextBefore,
                    context_after = request.contextAfter
                )
            )

            AiTaskType.SUMMARIZE -> AIRetrofitClient.instance.summarize(
                AITextRequest(
                    text = request.selectedText,
                    book_name = request.bookName,
                    context_before = request.contextBefore,
                    context_after = request.contextAfter
                )
            )
        }

        activeAiCall = call
        call.enqueue(object : Callback<AITextResponse> {
            override fun onResponse(call: Call<AITextResponse>, response: Response<AITextResponse>) {
                if (activeAiCall !== call) return
                activeAiCall = null
                if (!dialog.isShowing || isFinishing) return

                val result = response.body()?.result?.takeIf { it.isNotBlank() }
                if (response.isSuccessful && result != null) {
                    renderAiSuccessState(views, normalizeAiResultText(result))
                } else {
                    renderAiErrorState(
                        views,
                        mapAiErrorMessage(response.code()),
                        onRetry = { executeAiRequest(dialog, views, request) }
                    )
                }
            }

            override fun onFailure(call: Call<AITextResponse>, t: Throwable) {
                if (call.isCanceled) return
                if (activeAiCall === call) {
                    activeAiCall = null
                }
                if (!dialog.isShowing || isFinishing) return

                val message = if (t is IOException) {
                    getString(R.string.reader_ai_error_network)
                } else {
                    getString(R.string.reader_ai_error_unknown)
                }
                renderAiErrorState(
                    views,
                    message,
                    onRetry = { executeAiRequest(dialog, views, request) }
                )
            }
        })
    }

    private fun renderAiLoadingState(
        views: AiSheetViews,
        request: ReaderAiRequest
    ) {
        views.loadingLayout.isVisible = true
        views.errorLayout.isVisible = false
        views.resultLayout.isVisible = false
        views.loadingTitle.setText(R.string.reader_ai_loading_title)
        views.loadingHint.setText(request.task.loadingHintRes)

        views.secondaryButton.setText(R.string.reader_ai_button_close)
        views.secondaryButton.isEnabled = true

        // BUG-12 FIX: Previously showed "AI đang phân tích..." as button text which was
        // confusing (disabled button looks like a broken actionable element). Now hidden
        // until a result/error state is available.
        views.primaryButton.isVisible = false
        views.primaryButton.isEnabled = false
        views.primaryButton.setOnClickListener(null)
    }

    private fun renderAiSuccessState(
        views: AiSheetViews,
        result: String
    ) {
        views.loadingLayout.isVisible = false
        views.errorLayout.isVisible = false
        views.resultLayout.isVisible = true
        
        // Render markdown with Markwon
        val markwon = Markwon.create(this)
        markwon.setMarkdown(views.resultContent, result)

        views.secondaryButton.setText(R.string.reader_ai_button_close)
        views.secondaryButton.isEnabled = true

        // BUG-12 FIX: Make primary button visible again now that we have a result
        views.primaryButton.isVisible = true
        views.primaryButton.alpha = 1f
        views.primaryButton.isEnabled = true
        views.primaryButton.setText(R.string.reader_ai_button_copy)
        views.primaryButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("AI Result", result))
            Toast.makeText(this, R.string.reader_ai_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderAiErrorState(
        views: AiSheetViews,
        message: String,
        onRetry: () -> Unit
    ) {
        views.loadingLayout.isVisible = false
        views.errorLayout.isVisible = true
        views.resultLayout.isVisible = false
        views.errorMessage.text = message

        views.secondaryButton.setText(R.string.reader_ai_button_close)
        views.secondaryButton.isEnabled = true

        // BUG-12 FIX: Make primary button visible again in error state
        views.primaryButton.isVisible = true
        views.primaryButton.alpha = 1f
        views.primaryButton.isEnabled = true
        views.primaryButton.setText(R.string.reader_ai_button_retry)
        views.primaryButton.setOnClickListener { onRetry() }
    }

    private fun normalizeAiResultText(raw: String): String {
        // We no longer strip formatting, let Markwon render it.
        return raw
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun mapAiErrorMessage(code: Int): String {
        return when (code) {
            400, 422 -> getString(R.string.reader_ai_invalid_selection)
            in 500..599 -> getString(R.string.reader_ai_error_server)
            else -> getString(R.string.reader_ai_error_unknown)
        }
    }
}
