package com.bookapp.ui.book

import android.os.Bundle
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.api.EnsureReadingProgressRequest
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.api.UpdateReadingProgressRequest
import com.bookapp.data.model.Chapter
import com.bookapp.data.model.ReadingProgress
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.R as MaterialR
import kotlin.math.abs
import kotlin.math.roundToInt
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReaderActivity : AppCompatActivity() {

    private enum class ReadingMode {
        SCROLL, PAGE
    }

    private data class FontOption(
        val label: String,
        val family: String
    )

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
        const val EXTRA_TARGET_CHAPTER_ID = "extra_target_chapter_id"
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

    private var backgroundColor: Int = 0xFFFDFDFD.toInt()
    private var fontSizeSp: Float = 18f
    private var lineSpacingMultiplier: Float = 1.5f
    private var readingMode: ReadingMode = ReadingMode.SCROLL
    private var fontFamily: String = "sans-serif"
    private var autoScrollSpeed: Int = 30
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
                // Keep the same speed index but reduce effective vertical speed by half.
                val step = (autoScrollSpeed / 2).coerceIn(2, 40)
                readerScroll.smoothScrollBy(0, step)
                autoPlayHandler.postDelayed(this, 80L)
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
        FontOption("HelveticaNeue", "sans-serif"),
        FontOption("Palatino", "serif"),
        FontOption("ArialMT", "sans-serif"),
        FontOption("AvenirNext-Medium", "sans-serif-medium"),
        FontOption("Bookerly", "serif"),
        FontOption("TimesNewRoman", "serif"),
        FontOption("Georgia", "serif"),
        FontOption("Courier", "monospace"),
        FontOption("Roboto-Regular", "sans-serif"),
        FontOption("UTM-Centur", "serif"),
        FontOption("UVNVan", "sans-serif")
    )

    private val prefs by lazy {
        getSharedPreferences("ReaderSettings", MODE_PRIVATE)
    }

    private var downX = 0f
    private var downY = 0f
    private var dragDetected = false
    private var pageSwipeHandled = false
    private var pageTurnAnimating = false
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

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

        val touchListener = View.OnTouchListener { _, event ->
            if (readingMode == ReadingMode.PAGE) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        stopAutoPlay()
                        downX = event.x
                        downY = event.y
                        pageSwipeHandled = false
                        return@OnTouchListener true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Disable vertical scroll in page mode.
                        return@OnTouchListener true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (pageSwipeHandled || pageTurnAnimating) {
                            return@OnTouchListener true
                        }

                        val dx = event.x - downX
                        val dy = event.y - downY
                        val moved = abs(dx) > touchSlop || abs(dy) > touchSlop
                        val fromMiddle = downX >= tvContent.width * 0.2f && downX <= tvContent.width * 0.8f
                        val isHorizontalSwipe = fromMiddle && abs(dx) > (touchSlop * 2) && abs(dx) > abs(dy)

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
                        } else if (moved) {
                            if (controlsVisible) {
                                hideControls()
                            }
                        } else {
                            if (controlsVisible) {
                                hideControls()
                            } else {
                                showControlsAndRefresh()
                            }
                        }
                        return@OnTouchListener true
                    }
                }
                return@OnTouchListener true
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

        readerScroll.setOnTouchListener(touchListener)
        tvContent.setOnTouchListener(touchListener)
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

        currentChapterIndex = index
        val chapter = chapters[index]
        currentChapterRawText = chapter.content?.takeIf { it.isNotBlank() } ?: "(Chương này chưa có nội dung)"

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

            styleColorChip(colorAuto, backgroundColor == 0xFFFDFDFD.toInt())
            styleColorBlock(colorWhite, 0xFFFFFFFF.toInt(), backgroundColor == 0xFFFFFFFF.toInt())
            styleColorBlock(colorBlack, 0xFF1A1A1A.toInt(), backgroundColor == 0xFF1A1A1A.toInt())
            styleColorBlock(colorCream, 0xFFF1E5C8.toInt(), backgroundColor == 0xFFF1E5C8.toInt())
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
            backgroundColor = 0xFFFDFDFD.toInt()
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
            backgroundColor = 0xFFF1E5C8.toInt()
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
            if (chapterPages.size == 1) return 100
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
            tvContent.text = currentChapterRawText
            return
        }
        currentPageIndex = currentPageIndex.coerceIn(0, chapterPages.lastIndex)
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
            switchToChapter(currentChapterIndex - 1)
            readerScroll.post {
                currentPageIndex = chapterPages.lastIndex.coerceAtLeast(0)
                renderCurrentPageOnly()
            }
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
        backgroundColor = prefs.getInt("backgroundColor", 0xFFFDFDFD.toInt())
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
        target.setBackgroundResource(if (selected) R.drawable.btn_primary_bg else R.drawable.btn_outline_bg)
        target.setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFF23408E.toInt())
    }

    private fun styleColorChip(target: TextView, selected: Boolean) {
        target.setBackgroundResource(if (selected) R.drawable.btn_primary_bg else R.drawable.btn_outline_bg)
        target.setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFF23408E.toInt())
    }

    private fun styleColorBlock(target: View, color: Int, selected: Boolean) {
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 8f * resources.displayMetrics.density
            setStroke(
                if (selected) (3f * resources.displayMetrics.density).roundToInt() else 1,
                if (selected) 0xFF1F6FB2.toInt() else 0xFF9FA5AE.toInt()
            )
        }
        target.background = drawable
    }
}
