package com.bookapp.ui.book

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.api.AddCommentRequest
import com.bookapp.data.api.AddReviewRequest
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.api.ToggleFavoriteRequest
import com.bookapp.data.api.ToggleFavoriteResponse
import com.bookapp.data.api.FavoriteStatusResponse
import com.bookapp.data.model.Book
import com.bookapp.data.model.Chapter
import com.bookapp.data.model.Comment
import com.bookapp.data.model.Review
import com.bookapp.ui.feature.LibraryStorage
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.R as MaterialR
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
    }

    // Views
    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvRatingBadge: TextView
    private lateinit var tvChapters: TextView
    private lateinit var tvViews: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnFavorite: Button
    private lateinit var btnReadNow: Button
    private lateinit var btnChapterList: Button

    // Tabs
    private lateinit var tabIntro: Button
    private lateinit var tabReviews: Button
    private lateinit var tabComments: Button
    private lateinit var panelIntro: LinearLayout
    private lateinit var panelReviews: LinearLayout
    private lateinit var panelComments: LinearLayout

    // Reviews
    private lateinit var recyclerReviews: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var ratingBarInput: RatingBar
    private lateinit var edtReviewInput: EditText
    private lateinit var btnSubmitReview: Button
    private lateinit var tvNoReviews: TextView

    // Comments
    private lateinit var recyclerComments: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var edtCommentInput: EditText
    private lateinit var btnSubmitComment: Button
    private lateinit var tvNoComments: TextView

    private var bookId: String? = null
    private var isFavorited = false
    private var currentBook: Book? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        bookId = intent.getStringExtra(EXTRA_BOOK_ID)?.trim()?.takeIf { it.isNotEmpty() }

        if (bookId == null) {
            Toast.makeText(this, "Không thể mở chi tiết sách do thiếu ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val bindViewsSuccess = runCatching {
            bindViews()
        }.onFailure {
            Toast.makeText(this, "Không thể tải giao diện chi tiết sách", Toast.LENGTH_SHORT).show()
            finish()
        }.isSuccess

        if (!bindViewsSuccess) {
            return
        }

        loadBookDetail()
        checkFavoriteStatus()
    }

    private fun bindViews() {
        ivCover = findViewById(R.id.ivDetailCover)
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvAuthor = findViewById(R.id.tvDetailAuthor)
        tvCategory = findViewById(R.id.tvDetailCategory)
        tvRatingBadge = findViewById(R.id.tvDetailRatingBadge)
        tvChapters = findViewById(R.id.tvDetailChapters)
        tvViews = findViewById(R.id.tvDetailViews)
        tvDescription = findViewById(R.id.tvDetailDescription)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnReadNow = findViewById(R.id.btnReadNow)
        btnChapterList = findViewById(R.id.btnChapterList)

        tabIntro = findViewById(R.id.tabIntro)
        tabReviews = findViewById(R.id.tabReviews)
        tabComments = findViewById(R.id.tabComments)
        panelIntro = findViewById(R.id.panelIntro)
        panelReviews = findViewById(R.id.panelReviews)
        panelComments = findViewById(R.id.panelComments)

        recyclerReviews = findViewById(R.id.recyclerReviews)
        reviewAdapter = ReviewAdapter()
        recyclerReviews.layoutManager = LinearLayoutManager(this)
        recyclerReviews.adapter = reviewAdapter
        recyclerReviews.isNestedScrollingEnabled = false

        ratingBarInput = findViewById(R.id.ratingBarInput)
        edtReviewInput = findViewById(R.id.edtReviewInput)
        btnSubmitReview = findViewById(R.id.btnSubmitReview)
        tvNoReviews = findViewById(R.id.tvNoReviews)

        recyclerComments = findViewById(R.id.recyclerComments)
        commentAdapter = CommentAdapter()
        recyclerComments.layoutManager = LinearLayoutManager(this)
        recyclerComments.adapter = commentAdapter
        recyclerComments.isNestedScrollingEnabled = false

        edtCommentInput = findViewById(R.id.edtCommentInput)
        btnSubmitComment = findViewById(R.id.btnSubmitComment)
        tvNoComments = findViewById(R.id.tvNoComments)

        // Back
            findViewById<View>(R.id.btnDetailBack).setOnClickListener { finish() }

        // Tabs
        tabIntro.setOnClickListener { showTab("intro") }
        tabReviews.setOnClickListener {
            showTab("reviews")
            loadReviews()
        }
        tabComments.setOnClickListener {
            showTab("comments")
            loadComments()
        }

        // Favorite
        btnFavorite.setOnClickListener { toggleFavorite() }

        // Read now
        btnReadNow.setOnClickListener {
            val id = bookId ?: return@setOnClickListener
            addToRecentLibrary(id)
            val intent = Intent(this, ReaderActivity::class.java).apply {
                putExtra(ReaderActivity.EXTRA_BOOK_ID, id)
                putExtra(ReaderActivity.EXTRA_BOOK_TITLE, tvTitle.text.toString())
            }
            startActivity(intent)
        }

        btnChapterList.setOnClickListener {
            showChapterListDialog()
        }

        // Submit review
        btnSubmitReview.setOnClickListener { submitReview() }

        // Submit comment
        btnSubmitComment.setOnClickListener { submitComment() }
    }

    private fun loadBookDetail() {
        val id = bookId ?: return
        runCatching {
            RetrofitClient.instance.getBookById(id)
                .enqueue(object : Callback<Book> {
                    override fun onResponse(call: Call<Book>, response: Response<Book>) {
                        if (response.isSuccessful) {
                            response.body()?.let { bindBook(it) }
                        } else {
                            Toast.makeText(this@BookDetailActivity, "Không tải được thông tin sách", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Book>, t: Throwable) {
                        Toast.makeText(this@BookDetailActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }.onFailure {
            Toast.makeText(this, "Không thể tải chi tiết sách", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindBook(book: Book) {
        currentBook = book
        tvTitle.text = book.title ?: "Chưa có tiêu đề"
        tvAuthor.text = "Tác giả: ${book.author ?: "Không rõ"}"
        tvCategory.text = book.categoryId?.take(8) ?: "Chưa phân loại"
        tvRatingBadge.text = book.avgRating?.let { String.format("%.1f", it) } ?: "N/A"
        tvChapters.text = "${book.totalChapters ?: 0}"
        tvViews.text = "${book.views ?: 0}"
        tvDescription.text = book.description?.takeIf { it.isNotBlank() }?.let { decodeHtmlDescription(it) } ?: "(Chưa có mô tả)"

        if (!book.coverImage.isNullOrBlank()) {
            Glide.with(this)
                .load(book.coverImage)
                .placeholder(R.drawable.book_cover_placeholder)
                .error(R.drawable.book_cover_placeholder)
                .centerCrop()
                .into(ivCover)
        }
    }

    private fun checkFavoriteStatus() {
        val userId = getUserId() ?: return
        val id = bookId ?: return
        runCatching {
            RetrofitClient.instance.checkFavorite(userId, id)
                .enqueue(object : Callback<FavoriteStatusResponse> {
                    override fun onResponse(call: Call<FavoriteStatusResponse>, response: Response<FavoriteStatusResponse>) {
                        if (response.isSuccessful) {
                            isFavorited = response.body()?.favorited ?: false
                            updateFavoriteButton()
                        }
                    }
                    override fun onFailure(call: Call<FavoriteStatusResponse>, t: Throwable) {}
                })
        }
    }

    private fun toggleFavorite() {
        val userId = getUserId()
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            return
        }
        val id = bookId ?: return
        runCatching {
            RetrofitClient.instance.toggleFavorite(ToggleFavoriteRequest(userId, id))
                .enqueue(object : Callback<ToggleFavoriteResponse> {
                    override fun onResponse(call: Call<ToggleFavoriteResponse>, response: Response<ToggleFavoriteResponse>) {
                        if (response.isSuccessful) {
                            isFavorited = response.body()?.favorited ?: !isFavorited
                            updateFavoriteButton()
                            val libItem = buildLibraryItem(id)
                            if (libItem != null) {
                                if (isFavorited) {
                                    LibraryStorage.addFavorite(this@BookDetailActivity, libItem)
                                } else {
                                    LibraryStorage.removeFavorite(this@BookDetailActivity, id)
                                }
                            }
                            val msg = if (isFavorited) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích"
                            Toast.makeText(this@BookDetailActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ToggleFavoriteResponse>, t: Throwable) {
                        Toast.makeText(this@BookDetailActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }.onFailure {
            Toast.makeText(this, "Không thể cập nhật yêu thích", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFavoriteButton() {
        if (isFavorited) {
            btnFavorite.text = getString(R.string.favorite_active)
            btnFavorite.setBackgroundResource(R.drawable.btn_favorited_bg)
            btnFavorite.setTextColor(0xFFE53935.toInt())
            btnFavorite.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        } else {
            btnFavorite.text = getString(R.string.favorite_inactive)
            btnFavorite.setBackgroundResource(R.drawable.btn_outline_bg)
            btnFavorite.setTextColor(ContextCompat.getColor(this, R.color.text_accent))
            btnFavorite.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    private fun loadReviews() {
        val id = bookId ?: return
        runCatching {
            RetrofitClient.instance.getReviewsByBook(id)
                .enqueue(object : Callback<List<Review>> {
                    override fun onResponse(call: Call<List<Review>>, response: Response<List<Review>>) {
                        if (response.isSuccessful) {
                            val reviews = response.body().orEmpty()
                            reviewAdapter.submitList(reviews)
                            tvNoReviews.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                    override fun onFailure(call: Call<List<Review>>, t: Throwable) {}
                })
        }
    }

    private fun loadComments() {
        val id = bookId ?: return
        runCatching {
            RetrofitClient.instance.getCommentsByBook(id)
                .enqueue(object : Callback<List<Comment>> {
                    override fun onResponse(call: Call<List<Comment>>, response: Response<List<Comment>>) {
                        if (response.isSuccessful) {
                            val comments = response.body().orEmpty()
                            commentAdapter.submitList(comments)
                            tvNoComments.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                    override fun onFailure(call: Call<List<Comment>>, t: Throwable) {}
                })
        }
    }

    private fun submitReview() {
        val userId = getUserId()
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
            return
        }
        val id = bookId ?: return
        val rating = ratingBarInput.rating.toInt()
        val text = edtReviewInput.text.toString().trim()

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitReview.isEnabled = false
        runCatching {
            RetrofitClient.instance.addReview(AddReviewRequest(id, userId, rating, text))
                .enqueue(object : Callback<Review> {
                    override fun onResponse(call: Call<Review>, response: Response<Review>) {
                        btnSubmitReview.isEnabled = true
                        if (response.isSuccessful) {
                            edtReviewInput.setText("")
                            ratingBarInput.rating = 5f
                            Toast.makeText(this@BookDetailActivity, "Đã gửi đánh giá!", Toast.LENGTH_SHORT).show()
                            loadReviews()
                        } else {
                            Toast.makeText(this@BookDetailActivity, "Lỗi gửi đánh giá (HTTP ${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Review>, t: Throwable) {
                        btnSubmitReview.isEnabled = true
                        Toast.makeText(this@BookDetailActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }.onFailure {
            btnSubmitReview.isEnabled = true
            Toast.makeText(this, "Không thể gửi đánh giá", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitComment() {
        val userId = getUserId()
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show()
            return
        }
        val id = bookId ?: return
        val content = edtCommentInput.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "Nội dung bình luận không thể trống", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitComment.isEnabled = false
        runCatching {
            RetrofitClient.instance.addComment(AddCommentRequest(id, userId, content))
                .enqueue(object : Callback<Comment> {
                    override fun onResponse(call: Call<Comment>, response: Response<Comment>) {
                        btnSubmitComment.isEnabled = true
                        if (response.isSuccessful) {
                            edtCommentInput.setText("")
                            Toast.makeText(this@BookDetailActivity, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show()
                            loadComments()
                        } else {
                            Toast.makeText(this@BookDetailActivity, "Lỗi gửi bình luận (HTTP ${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Comment>, t: Throwable) {
                        btnSubmitComment.isEnabled = true
                        Toast.makeText(this@BookDetailActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }.onFailure {
            btnSubmitComment.isEnabled = true
            Toast.makeText(this, "Không thể gửi bình luận", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTab(tab: String) {
        val inactiveTabColor = ContextCompat.getColor(this, R.color.text_secondary)
        val activeTabColor = ContextCompat.getColor(this, android.R.color.white)

        // Reset all tabs
        listOf(tabIntro, tabReviews, tabComments).forEach {
            it.setBackgroundResource(android.R.color.transparent)
            it.setTextColor(inactiveTabColor)
        }
        panelIntro.visibility = View.GONE
        panelReviews.visibility = View.GONE
        panelComments.visibility = View.GONE

        when (tab) {
            "intro" -> {
                tabIntro.setBackgroundResource(R.drawable.btn_primary_bg)
                tabIntro.setTextColor(activeTabColor)
                panelIntro.visibility = View.VISIBLE
            }
            "reviews" -> {
                tabReviews.setBackgroundResource(R.drawable.btn_primary_bg)
                tabReviews.setTextColor(activeTabColor)
                panelReviews.visibility = View.VISIBLE
            }
            "comments" -> {
                tabComments.setBackgroundResource(R.drawable.btn_primary_bg)
                tabComments.setTextColor(activeTabColor)
                panelComments.visibility = View.VISIBLE
            }
        }
    }

    private fun getUserId(): String? {
        val prefs = getSharedPreferences("BookAppPrefs", MODE_PRIVATE)
        return prefs.getString("userId", null)
    }

    private fun showChapterListDialog() {
        val id = bookId ?: return

        RetrofitClient.instance.getChaptersByBook(id)
            .enqueue(object : Callback<List<Chapter>> {
                override fun onResponse(call: Call<List<Chapter>>, response: Response<List<Chapter>>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@BookDetailActivity, "Không tải được danh sách chương", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val chapters = response.body().orEmpty().filter { !it.id.isNullOrBlank() }
                    if (chapters.isEmpty()) {
                        Toast.makeText(this@BookDetailActivity, "Sách chưa có chương", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val dialog = BottomSheetDialog(this@BookDetailActivity)
                    val content = layoutInflater.inflate(R.layout.dialog_reader_chapters, null)
                    dialog.setContentView(content)

                    val pageSize = 15
                    content.findViewById<TextView>(R.id.tvDialogChapterTitle)?.text = tvTitle.text
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
                        val chapterId = selectedChapter.id ?: return@ReaderChapterAdapter
                        addToRecentLibrary(id)
                        val intent = Intent(this@BookDetailActivity, ReaderActivity::class.java).apply {
                            putExtra(ReaderActivity.EXTRA_BOOK_ID, id)
                            putExtra(ReaderActivity.EXTRA_BOOK_TITLE, tvTitle.text.toString())
                            putExtra(ReaderActivity.EXTRA_TARGET_CHAPTER_ID, chapterId)
                        }
                        startActivity(intent)
                    }

                    recycler.layoutManager = LinearLayoutManager(this@BookDetailActivity)
                    recycler.adapter = adapter

                    val totalPages = ((chapters.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                    var currentPage = 0

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

                        adapter.submitList(pageItems, null)
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

                override fun onFailure(call: Call<List<Chapter>>, t: Throwable) {
                    Toast.makeText(this@BookDetailActivity, "Lỗi tải chương: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addToRecentLibrary(bookId: String) {
        val item = buildLibraryItem(bookId)
        if (item != null) {
            LibraryStorage.addRecent(this, item)
        }
    }

    private fun buildLibraryItem(bookId: String): LibraryStorage.LibraryBookItem? {
        val book = currentBook
        return if (book != null) {
            LibraryStorage.fromBook(book, fallbackBookId = bookId, fallbackTitle = tvTitle.text.toString())
        } else {
            LibraryStorage.basicItem(bookId, tvTitle.text.toString())
        }
    }

    private fun decodeHtmlDescription(html: String): CharSequence {
        // Decode HTML entities first
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
        
        // Strip HTML tags (if any remain)
        val text = decoded.toString()
        return text.replace("<[^>]*>".toRegex(), "")  // Remove HTML tags
            .replace("&[a-zA-Z]+;".toRegex(), "")     // Remove remaining HTML entities
            .replace("&#\\d+;".toRegex(), "")         // Remove numeric entities
            .replace("&#x[0-9a-fA-F]+;".toRegex(), "")  // Remove hex entities
            .trim()
    }
}
