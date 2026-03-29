package com.bookapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bookapp.R
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.model.Book
import com.bookapp.ui.admin.AdminActivity
import com.bookapp.ui.auth.LoginActivity
import com.bookapp.ui.feature.FeatureActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private lateinit var bookAdapter: BookAdapter
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var tvPageIndicator: TextView
    private lateinit var tvBooksError: TextView

    private val allBooks = mutableListOf<Book>()
    private var currentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bindBookGrid()
        bindFeatureButtons()
        bindBottomNavigation()
        loadBooks()
    }

    private fun bindBookGrid() {
        val recyclerBooks = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerBooks)
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
        tvBooksError = findViewById(R.id.tvBooksError)

        bookAdapter = BookAdapter()
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

    private fun renderPage() {
        if (allBooks.isEmpty()) {
            bookAdapter.submitList(emptyList())
            tvPageIndicator.text = "Trang 1/1"
            btnPrevPage.isEnabled = false
            btnNextPage.isEnabled = false
            showError("Chua co sach trong he thong")
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
        findViewById<Button>(R.id.btnContinueRead).setOnClickListener {
            openFeature("Tiep tuc doc", "Mo lai sach dang doc va tiep tuc hanh trinh cua ban.")
        }
        findViewById<Button>(R.id.btnLibrary).setOnClickListener {
            openFeature("Thu vien", "Quan ly tat ca sach ban da mua, dang doc va da doc.")
        }
        findViewById<Button>(R.id.btnCategories).setOnClickListener {
            openFeature("The loai", "Kham pha sach theo chu de va the loai yeu thich.")
        }
        findViewById<Button>(R.id.btnFavorites).setOnClickListener {
            openFeature("Yeu thich", "Xem danh sach sach ban da danh dau yeu thich.")
        }
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            openFeature("Lich su doc", "Theo doi tien trinh va lich su doc gan day.")
        }
        findViewById<TextView>(R.id.btnSeeAllRecommended).setOnClickListener {
            openFeature("De xuat", "Danh sach de xuat sach theo so thich cua ban.")
        }
        findViewById<Button>(R.id.btnAdminPanel).setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun bindBottomNavigation() {
        findViewById<Button>(R.id.navHome).setOnClickListener {
            Toast.makeText(this, "Ban dang o trang Home", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.navExplore).setOnClickListener {
            openFeature("Kham pha", "Xem bang xep hang, xu huong va sach moi phat hanh.")
        }
        findViewById<Button>(R.id.navLibrary).setOnClickListener {
            openFeature("Thu vien", "Truy cap thu vien ca nhan cua ban.")
        }
        findViewById<Button>(R.id.navProfile).setOnClickListener {
            openFeature("Tai khoan", "Quan ly thong tin tai khoan, goi cuoc va cai dat.")
        }
    }

    private fun openFeature(title: String, description: String) {
        val intent = Intent(this, FeatureActivity::class.java).apply {
            putExtra(FeatureActivity.EXTRA_TITLE, title)
            putExtra(FeatureActivity.EXTRA_DESCRIPTION, description)
        }
        startActivity(intent)
    }

    companion object {
        private const val BOOKS_PER_PAGE = 10
    }
}