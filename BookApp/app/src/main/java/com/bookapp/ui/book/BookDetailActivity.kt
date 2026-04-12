package com.bookapp.ui.book

import android.content.Intent
import android.os.Bundle
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
    private lateinit var tvPages: TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        bookId = intent.getStringExtra(EXTRA_BOOK_ID)?.trim()?.takeIf { it.isNotEmpty() }

        if (bookId == null) {
            Toast.makeText(this, "Khong the mo chi tiet sach do thieu ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val bindViewsSuccess = runCatching {
            bindViews()
        }.onFailure {
            Toast.makeText(this, "Khong the tai giao dien chi tiet sach", Toast.LENGTH_SHORT).show()
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
        tvPages = findViewById(R.id.tvDetailPages)
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
                            Toast.makeText(this@BookDetailActivity, "Khong tai duoc thong tin sach", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Book>, t: Throwable) {
                        Toast.makeText(this@BookDetailActivity, "Loi ket noi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }.onFailure {
            Toast.makeText(this, "Khong the tai chi tiet sach", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindBook(book: Book) {
        tvTitle.text = book.title ?: "Chua co tieu de"
        tvAuthor.text = "Tac gia: ${book.author ?: "Khong ro"}"
        tvCategory.text = book.categoryId?.take(8) ?: "Chua phan loai"
        tvRatingBadge.text = book.avgRating?.let { String.format("%.1f", it) } ?: "N/A"
        tvChapters.text = "${book.totalChapters ?: 0}"
        tvPages.text = "${book.totalPages ?: 0}"
        tvViews.text = "${book.views ?: 0}"
        tvDescription.text = book.description?.takeIf { it.isNotBlank() } ?: "(Chua co mo ta)"

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
            Toast.makeText(this, "Vui long dang nhap de su dung tinh nang nay", Toast.LENGTH_SHORT).show()
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
                            val msg = if (isFavorited) "Da them vao yeu thich" else "Da xoa khoi yeu thich"
                            Toast.makeText(this@BookDetailActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ToggleFavoriteResponse>, t: Throwable) {
                        Toast.makeText(this@BookDetailActivity, "Loi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }.onFailure {
            Toast.makeText(this, "Khong the cap nhat yeu thich", Toast.LENGTH_SHORT).show()
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
            btnFavorite.setTextColor(0xFF23408E.toInt())
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
            Toast.makeText(this, "Vui long dang nhap de danh gia", Toast.LENGTH_SHORT).show()
            return
        }
        val id = bookId ?: return
        val rating = ratingBarInput.rating.toInt()
        val text = edtReviewInput.text.toString().trim()

        if (rating == 0) {
            Toast.makeText(this, "Vui long chon so sao", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@BookDetailActivity, "Da gui danh gia!", Toast.LENGTH_SHORT).show()
                            loadReviews()
                        } else {
                            Toast.makeText(this@BookDetailActivity, "Loi gui danh gia (HTTP ${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Review>, t: Throwable) {
                        btnSubmitReview.isEnabled = true
                        Toast.makeText(this@BookDetailActivity, "Loi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }.onFailure {
            btnSubmitReview.isEnabled = true
            Toast.makeText(this, "Khong the gui danh gia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitComment() {
        val userId = getUserId()
        if (userId == null) {
            Toast.makeText(this, "Vui long dang nhap de binh luan", Toast.LENGTH_SHORT).show()
            return
        }
        val id = bookId ?: return
        val content = edtCommentInput.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "Noi dung binh luan khong the trong", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@BookDetailActivity, "Da gui binh luan!", Toast.LENGTH_SHORT).show()
                            loadComments()
                        } else {
                            Toast.makeText(this@BookDetailActivity, "Loi gui binh luan (HTTP ${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Comment>, t: Throwable) {
                        btnSubmitComment.isEnabled = true
                        Toast.makeText(this@BookDetailActivity, "Loi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }.onFailure {
            btnSubmitComment.isEnabled = true
            Toast.makeText(this, "Khong the gui binh luan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTab(tab: String) {
        // Reset all tabs
        listOf(tabIntro, tabReviews, tabComments).forEach {
            it.setBackgroundResource(android.R.color.transparent)
            it.setTextColor(0xFF68707A.toInt())
        }
        panelIntro.visibility = View.GONE
        panelReviews.visibility = View.GONE
        panelComments.visibility = View.GONE

        when (tab) {
            "intro" -> {
                tabIntro.setBackgroundResource(R.drawable.btn_primary_bg)
                tabIntro.setTextColor(0xFFFFFFFF.toInt())
                panelIntro.visibility = View.VISIBLE
            }
            "reviews" -> {
                tabReviews.setBackgroundResource(R.drawable.btn_primary_bg)
                tabReviews.setTextColor(0xFFFFFFFF.toInt())
                panelReviews.visibility = View.VISIBLE
            }
            "comments" -> {
                tabComments.setBackgroundResource(R.drawable.btn_primary_bg)
                tabComments.setTextColor(0xFFFFFFFF.toInt())
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
                        Toast.makeText(this@BookDetailActivity, "Khong tai duoc danh sach chuong", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val chapters = response.body().orEmpty().filter { !it.id.isNullOrBlank() }
                    if (chapters.isEmpty()) {
                        Toast.makeText(this@BookDetailActivity, "Sach chua co chuong", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@BookDetailActivity, "Loi tai chuong: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
