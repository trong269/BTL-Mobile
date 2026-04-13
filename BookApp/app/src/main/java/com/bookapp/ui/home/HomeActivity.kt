package com.bookapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.model.Book
import com.bookapp.ui.book.BookDetailActivity
import com.bookapp.ui.book.BookCatalogActivity
import com.bookapp.ui.feature.FeatureActivity
import com.bookapp.ui.profile.ProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private lateinit var bookAdapter: BookAdapter
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var tvPageIndicator: TextView
    private lateinit var tvBooksError: TextView
    private lateinit var btnTopWeekTab: Button
    private lateinit var btnTopMonthTab: Button
    private lateinit var tvTopDescription: TextView
    private lateinit var topBookAdapter: TopBookAdapter

    private val allBooks = mutableListOf<Book>()
    private var currentPage = 1
    private var selectedTopPeriod = TOP_PERIOD_WEEK
    private var topBooksWeek: List<Book> = emptyList()
    private var topBooksMonth: List<Book> = emptyList()
    private var lastOpenBookDetailMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bindBookGrid()
        bindTopRankings()
        bindFeatureButtons()
        bindBottomNavigation()
        loadBooks()
        loadTopBooksFromDatabase()
    }

    private fun bindBookGrid() {
        val recyclerBooks = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerBooks)
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
        tvBooksError = findViewById(R.id.tvBooksError)

        bookAdapter = BookAdapter { book ->
            openBookDetail(book)
        }
        recyclerBooks.layoutManager = GridLayoutManager(this, 2)
        recyclerBooks.adapter = bookAdapter

        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                renderPage()
            }
        }

        btnNextPage.setOnClickListener {
            val totalPages = getTotalPages()
            if (currentPage < totalPages) {
                currentPage++
                renderPage()
            }
        }
    }

    private fun bindTopRankings() {
        val recyclerTopBooks = findViewById<RecyclerView>(R.id.recyclerTopBooks)
        btnTopWeekTab = findViewById(R.id.btnTopWeekTab)
        btnTopMonthTab = findViewById(R.id.btnTopMonthTab)
        tvTopDescription = findViewById(R.id.tvTopDescription)

        topBookAdapter = TopBookAdapter("7 ngay") { book ->
            openBookDetail(book)
        }
        recyclerTopBooks.layoutManager = LinearLayoutManager(this)
        recyclerTopBooks.adapter = topBookAdapter

        btnTopWeekTab.setOnClickListener {
            selectedTopPeriod = TOP_PERIOD_WEEK
            renderSelectedTopList()
        }

        btnTopMonthTab.setOnClickListener {
            selectedTopPeriod = TOP_PERIOD_MONTH
            renderSelectedTopList()
        }

        renderSelectedTopList()
    }

    private fun loadBooks() {
        showError("Dang tai du lieu sach...")

        RetrofitClient.instance.getAllBooks()
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        val books = response.body().orEmpty()
                        allBooks.clear()
                        allBooks.addAll(books)
                        currentPage = 1
                        renderPage()
                    } else {
                        showError("Khong tai duoc danh sach sach (HTTP ${response.code()})")
                    }
                }

                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    showError("Loi ket noi: ${t.message ?: "Khong xac dinh"}")
                }
            })
    }

    private fun loadTopBooksFromDatabase() {
        RetrofitClient.instance.getTopBooksWeek()
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        topBooksWeek = response.body().orEmpty()
                        renderSelectedTopList()
                    }
                }

                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    topBooksWeek = emptyList()
                    renderSelectedTopList()
                }
            })

        RetrofitClient.instance.getTopBooksMonth()
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        topBooksMonth = response.body().orEmpty()
                        renderSelectedTopList()
                    }
                }

                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    topBooksMonth = emptyList()
                    renderSelectedTopList()
                }
            })
    }

    private fun renderPage() {
        if (allBooks.isEmpty()) {
            bookAdapter.submitList(emptyList())
            tvPageIndicator.text = "Trang 1/1"
            btnPrevPage.isEnabled = false
            btnNextPage.isEnabled = false
            showError("Chua co sach trong he thong")
            topBooksWeek = emptyList()
            topBooksMonth = emptyList()
            renderSelectedTopList()
            return
        }

        tvBooksError.visibility = View.GONE

        val totalPages = getTotalPages()
        currentPage = currentPage.coerceIn(1, totalPages)

        val start = (currentPage - 1) * BOOKS_PER_PAGE
        val end = minOf(start + BOOKS_PER_PAGE, allBooks.size)

        bookAdapter.submitList(allBooks.subList(start, end))
        tvPageIndicator.text = "Trang $currentPage/$totalPages"
        btnPrevPage.isEnabled = currentPage > 1
        btnNextPage.isEnabled = currentPage < totalPages
    }

    private fun renderSelectedTopList() {
        val isWeekSelected = selectedTopPeriod == TOP_PERIOD_WEEK

        val activeBackground = R.drawable.home_primary_button_bg
        val inactiveBackground = R.drawable.home_chip_bg
        val activeTextColor = ContextCompat.getColor(this, android.R.color.white)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.home_primary_dark)

        btnTopWeekTab.setBackgroundResource(if (isWeekSelected) activeBackground else inactiveBackground)
        btnTopMonthTab.setBackgroundResource(if (isWeekSelected) inactiveBackground else activeBackground)
        btnTopWeekTab.setTextColor(if (isWeekSelected) activeTextColor else inactiveTextColor)
        btnTopMonthTab.setTextColor(if (isWeekSelected) inactiveTextColor else activeTextColor)

        if (isWeekSelected) {
            tvTopDescription.text = "Cap nhat xu huong trong 7 ngay gan nhat"
            topBookAdapter.submitList(topBooksWeek, "7 ngay")
        } else {
            tvTopDescription.text = "Nhung dau sach noi bat nhat trong 30 ngay"
            topBookAdapter.submitList(topBooksMonth, "30 ngay")
        }
    }

    private fun getTotalPages(): Int {
        return if (allBooks.isEmpty()) {
            1
        } else {
            (allBooks.size + BOOKS_PER_PAGE - 1) / BOOKS_PER_PAGE
        }
    }

    private fun showError(message: String) {
        tvBooksError.visibility = View.VISIBLE
        tvBooksError.text = message
    }

    private fun bindFeatureButtons() {
        val edtSearch = findViewById<EditText>(R.id.edtSearch)
        val btnSearchIcon = findViewById<android.widget.ImageButton>(R.id.btnSearchIcon)

        val performSearch = {
            val query = edtSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                val intent = Intent(this, BookCatalogActivity::class.java).apply {
                    putExtra(BookCatalogActivity.EXTRA_QUERY, query)
                }
                startActivity(intent)
            }
        }

        // Search via keyboard
        edtSearch.setOnEditorActionListener { _, actionId, event ->
            val isKeyboardSearch = actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
            val isEnterKey = actionId == EditorInfo.IME_NULL &&
                event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN

            if (isKeyboardSearch || isEnterKey) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Search via button click
        btnSearchIcon.setOnClickListener {
            performSearch()
        }

        findViewById<Button>(R.id.btnContinueRead).setOnClickListener {
            openFeature("Tiep tuc doc", "Mo lai sach dang doc va tiep tuc hanh trinh cua ban.")
        }
        findViewById<Button>(R.id.btnLibrary).setOnClickListener {
            openFeature("Thu vien", "Quan ly tat ca sach ban da mua, dang doc va da doc.")
        }
        findViewById<Button>(R.id.btnCategories).setOnClickListener {
            val intent = Intent(this, BookCatalogActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnFavorites).setOnClickListener {
            openFeature("Yeu thich", "Xem danh sach sach ban da danh dau yeu thich.")
        }
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            openFeature("Lich su doc", "Theo doi tien trinh va lich su doc gan day.")
        }
        findViewById<TextView>(R.id.btnSeeAllRecommended).setOnClickListener {
            val intent = Intent(this, BookCatalogActivity::class.java).apply {
                putExtra(BookCatalogActivity.EXTRA_MODE, "all")
            }
            startActivity(intent)
        }
//        findViewById<Button>(R.id.btnAdminPanel).setOnClickListener {
//            startActivity(Intent(this, AdminActivity::class.java))
//        }
//        findViewById<Button>(R.id.btnLogout).setOnClickListener {
//            startActivity(Intent(this, LoginActivity::class.java))
//            finish()
//        }
    }

    private fun bindBottomNavigation() {
        findViewById<Button>(R.id.navHome).setOnClickListener {
            Toast.makeText(this, "Ban dang o trang Home", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.navExplore).setOnClickListener {
            val intent = Intent(this, BookCatalogActivity::class.java).apply {
                putExtra(BookCatalogActivity.EXTRA_MODE, "all")
            }
            startActivity(intent)
        }
        findViewById<Button>(R.id.navLibrary).setOnClickListener {
            openFeature("Thu vien", "Truy cap thu vien ca nhan cua ban.")
        }
        findViewById<Button>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun openFeature(title: String, description: String) {
        val intent = Intent(this, FeatureActivity::class.java).apply {
            putExtra(FeatureActivity.EXTRA_TITLE, title)
            putExtra(FeatureActivity.EXTRA_DESCRIPTION, description)
        }
        startActivity(intent)
    }

    private fun openBookDetail(book: Book) {
        val now = System.currentTimeMillis()
        if (now - lastOpenBookDetailMs < 600L) return

        val id = book.id?.trim()
        if (id.isNullOrEmpty()) {
            Toast.makeText(this, "Sach nay chua co ID hop le", Toast.LENGTH_SHORT).show()
            return
        }

        lastOpenBookDetailMs = now
        val intent = Intent(this, BookDetailActivity::class.java).apply {
            putExtra(BookDetailActivity.EXTRA_BOOK_ID, id)
            putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, book.title ?: "Chi tiet sach")
        }
        startActivity(intent)
    }

    companion object {
        private const val BOOKS_PER_PAGE = 10
        private const val TOP_PERIOD_WEEK = "WEEK"
        private const val TOP_PERIOD_MONTH = "MONTH"
    }
}