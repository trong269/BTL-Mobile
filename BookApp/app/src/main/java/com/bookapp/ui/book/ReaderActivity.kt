package com.bookapp.ui.book

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
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
import kotlin.math.abs
import kotlin.math.roundToInt
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
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
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnPrevChapter: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnPlay: ImageButton
    private lateinit var btnChapterList: ImageButton
    private lateinit var btnNextChapter: ImageButton

    private var userId: String? = null
    private var bookId: String? = null
    private var bookTitle: String = "Doc sach"

    private var chapters: List<Chapter> = emptyList()
    private var currentChapterIndex: Int = 0
    private var controlsVisible: Boolean = true

    private var downX = 0f
    private var downY = 0f
    private var dragDetected = false
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        bookId = intent.getStringExtra(EXTRA_BOOK_ID)?.trim()?.takeIf { it.isNotEmpty() }
        bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE)?.takeIf { it.isNotBlank() } ?: "Doc sach"
        userId = getSharedPreferences("BookAppPrefs", MODE_PRIVATE).getString("userId", null)

        if (bookId == null || userId == null) {
            Toast.makeText(this, "Thieu thong tin de mo trinh doc", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupInteractions()
        loadChaptersAndProgress()
    }

    override fun onPause() {
        super.onPause()
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
        btnRefresh = findViewById(R.id.btnReaderRefresh)
        btnPrevChapter = findViewById(R.id.btnReaderPrev)
        btnSettings = findViewById(R.id.btnReaderSettings)
        btnPlay = findViewById(R.id.btnReaderPlay)
        btnChapterList = findViewById(R.id.btnReaderList)
        btnNextChapter = findViewById(R.id.btnReaderNext)

        tvBookTitle.text = bookTitle
        seekChapterProgress.max = 100
        seekChapterProgress.progress = 0
        seekChapterProgress.setOnTouchListener { _, _ -> true }
    }

    private fun setupInteractions() {
        btnClose.setOnClickListener {
            persistCurrentProgress()
            finish()
        }

        btnRefresh.setOnClickListener {
            showControlsAndRefresh()
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
            Toast.makeText(this, "Setting se duoc lam sau", Toast.LENGTH_SHORT).show()
        }

        btnPlay.setOnClickListener {
            Toast.makeText(this, "Play se duoc lam sau", Toast.LENGTH_SHORT).show()
        }

        val touchListener = View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
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
                        Toast.makeText(this@ReaderActivity, "Khong tai duoc danh sach chuong", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    val loaded = response.body().orEmpty().filter { !it.id.isNullOrBlank() }
                    if (loaded.isEmpty()) {
                        Toast.makeText(this@ReaderActivity, "Sach chua co chuong", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                    chapters = loaded
                    ensureReadingProgress()
                }

                override fun onFailure(call: Call<List<Chapter>>, t: Throwable) {
                    Toast.makeText(this@ReaderActivity, "Loi tai chuong: ${t.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@ReaderActivity, "Khong tao duoc tien trinh doc", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val progress = response.body()
                val targetChapterId = progress?.chapterId
                val targetIndex = chapters.indexOfFirst { it.id == targetChapterId }.takeIf { it >= 0 } ?: 0
                currentChapterIndex = targetIndex
                val percent = (progress?.chapterProgressPercent ?: 0).coerceIn(0, 100)
                showChapter(targetIndex, percent)
            }

            override fun onFailure(call: Call<ReadingProgress>, t: Throwable) {
                Toast.makeText(this@ReaderActivity, "Loi tien trinh: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun showChapter(index: Int, initialPercent: Int) {
        if (chapters.isEmpty() || index !in chapters.indices) return

        currentChapterIndex = index
        val chapter = chapters[index]
        tvContent.text = chapter.content?.takeIf { it.isNotBlank() } ?: "(Chuong nay chua co noi dung)"

        val chapterTitle = chapter.title?.takeIf { it.isNotBlank() } ?: "(Khong co tieu de)"
        tvFooterChapterTitle.text = chapterTitle
        tvFooterChapterMeta.text = ""
        tvFooterChapterCounter.text = "${index + 1}/${chapters.size}"

        btnPrevChapter.isEnabled = index > 0
        btnNextChapter.isEnabled = index < chapters.lastIndex
        btnPrevChapter.alpha = if (btnPrevChapter.isEnabled) 1f else 0.35f
        btnNextChapter.alpha = if (btnNextChapter.isEnabled) 1f else 0.35f

        readerScroll.post {
            val child = readerScroll.getChildAt(0)
            if (child != null) {
                val range = (child.height - readerScroll.height).coerceAtLeast(0)
                val targetY = if (range <= 0) 0 else ((range * (initialPercent / 100f))).roundToInt()
                readerScroll.scrollTo(0, targetY)
            } else {
                readerScroll.scrollTo(0, 0)
            }
            showControlsAndRefresh()
        }
    }

    private fun switchToChapter(targetIndex: Int) {
        if (targetIndex !in chapters.indices) return

        currentChapterIndex = targetIndex
        val chapterId = chapters[targetIndex].id ?: return

        updateReadingProgress(chapterId, 0, onSuccess = {
            showChapter(targetIndex, 0)
        })
    }

    private fun showChapterBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val content = layoutInflater.inflate(R.layout.dialog_reader_chapters, null)
        dialog.setContentView(content)

        val recycler = content.findViewById<RecyclerView>(R.id.recyclerReaderChapters)
        val adapter = ReaderChapterAdapter { selectedIndex ->
            dialog.dismiss()
            if (selectedIndex != currentChapterIndex) {
                switchToChapter(selectedIndex)
            }
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        adapter.submitList(chapters, currentChapterIndex)

        dialog.show()
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
    }

    private fun calculateCurrentProgressPercent(): Int {
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
}
