package com.bookapp.ui.book

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.model.Book
import com.bookapp.data.model.Category
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookCatalogActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY_ID = "extra_category_id"
        const val EXTRA_QUERY = "extra_query"
        const val EXTRA_MODE = "extra_mode"   // "all" | "featured" | "new"
        private const val PAGE_SIZE = 12
    }

    private lateinit var recyclerCatalog: RecyclerView
    private lateinit var catalogAdapter: BookCatalogAdapter
    private lateinit var llCategoryTabs: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var tvPage: TextView
    private lateinit var edtSearch: EditText

    private val allBooks = mutableListOf<Book>()
    private val categories = mutableListOf<Category>()
    private var selectedCategoryId: String? = null  // null = Tất cả
    private var currentPage = 1
    private var currentMode = "all"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_catalog)

        currentMode = intent.getStringExtra(EXTRA_MODE) ?: "all"
        selectedCategoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
        searchQuery = intent.getStringExtra(EXTRA_QUERY) ?: ""

        bindViews()
        loadCategories()

        if (searchQuery.isNotBlank()) {
            edtSearch.setText(searchQuery)
            loadSearchResults(searchQuery)
        } else if (selectedCategoryId != null) {
            loadBooksByCategory(selectedCategoryId!!)
        } else {
            loadBooksByMode(currentMode)
        }
    }

    private fun bindViews() {
        recyclerCatalog = findViewById(R.id.recyclerCatalog)
        llCategoryTabs = findViewById(R.id.llCategoryTabs)
        tvStatus = findViewById(R.id.tvCatalogStatus)
        btnPrev = findViewById(R.id.btnCatalogPrev)
        btnNext = findViewById(R.id.btnCatalogNext)
        tvPage = findViewById(R.id.tvCatalogPage)
        edtSearch = findViewById(R.id.edtCatalogSearch)

        catalogAdapter = BookCatalogAdapter { categoryId ->
            categories.find { it.id == categoryId }?.name ?: ""
        }
        recyclerCatalog.layoutManager = LinearLayoutManager(this)
        recyclerCatalog.adapter = catalogAdapter

        // Back button
        findViewById<View>(R.id.btnCatalogBack).setOnClickListener { finish() }

        // Pagination
        btnPrev.setOnClickListener {
            if (currentPage > 1) { currentPage--; renderPage() }
        }
        btnNext.setOnClickListener {
            val total = getTotalPages()
            if (currentPage < total) { currentPage++; renderPage() }
        }

        // Search
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                if (q.length >= 2) {
                    loadSearchResults(q)
                } else if (q.isEmpty()) {
                    selectedCategoryId = null
                    loadBooksByMode(currentMode)
                    refreshCategoryTabs()
                }
            }
        })
    }

    private fun loadCategories() {
        RetrofitClient.instance.getAllCategories()
            .enqueue(object : Callback<List<Category>> {
                override fun onResponse(call: Call<List<Category>>, response: Response<List<Category>>) {
                    if (response.isSuccessful) {
                        categories.clear()
                        categories.addAll(response.body().orEmpty())
                        buildCategoryTabs()
                    }
                }
                override fun onFailure(call: Call<List<Category>>, t: Throwable) {}
            })
    }

    private fun buildCategoryTabs() {
        llCategoryTabs.removeAllViews()

        // Tab "Tất cả"
        addTab("Tat ca", null)
        // Tab "Nổi bật"
        addTabMode("Noi bat", "featured")
        // Tab "Mới nhất"
        addTabMode("Moi nhat", "new")
        // Tabs thể loại
        categories.forEach { cat -> addTab(cat.name ?: "", cat.id) }
    }

    private fun addTab(label: String, categoryId: String?) {
        val btn = Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            val isSelected = (categoryId == selectedCategoryId && searchQuery.isEmpty())
            setBackgroundResource(if (isSelected) R.drawable.chip_selected_bg else R.drawable.chip_unselected_bg)
            setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF23408E.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 8 }
            layoutParams = lp
            setPadding(40, 10, 40, 10)
            setOnClickListener {
                selectedCategoryId = categoryId
                searchQuery = ""
                edtSearch.setText("")
                if (categoryId == null) {
                    loadBooksByMode(currentMode)
                } else {
                    loadBooksByCategory(categoryId)
                }
                refreshCategoryTabs()
            }
        }
        llCategoryTabs.addView(btn)
    }

    private fun addTabMode(label: String, mode: String) {
        val btn = Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            val isSelected = (selectedCategoryId == null && currentMode == mode && searchQuery.isEmpty())
            setBackgroundResource(if (isSelected) R.drawable.chip_selected_bg else R.drawable.chip_unselected_bg)
            setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF23408E.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 8 }
            layoutParams = lp
            setPadding(40, 10, 40, 10)
            setOnClickListener {
                selectedCategoryId = null
                currentMode = mode
                searchQuery = ""
                edtSearch.setText("")
                loadBooksByMode(mode)
                refreshCategoryTabs()
            }
        }
        llCategoryTabs.addView(btn)
    }

    private fun refreshCategoryTabs() {
        buildCategoryTabs()
    }

    private fun loadBooksByMode(mode: String) {
        setStatus("Dang tai...")
        val call: Call<List<Book>> = when (mode) {
            "featured" -> RetrofitClient.instance.getFeaturedBooks()
            "new" -> RetrofitClient.instance.getNewBooks()
            else -> RetrofitClient.instance.getAllBooks()
        }
        call.enqueue(object : Callback<List<Book>> {
            override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                if (response.isSuccessful) {
                    updateBooks(response.body().orEmpty())
                } else {
                    setStatus("Loi tai sach (HTTP ${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                setStatus("Loi ket noi: ${t.message}")
            }
        })
    }

    private fun loadBooksByCategory(categoryId: String) {
        setStatus("Dang tai...")
        RetrofitClient.instance.getBooksByCategory(categoryId)
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        updateBooks(response.body().orEmpty())
                    } else {
                        setStatus("Loi tai sach (HTTP ${response.code()})")
                    }
                }
                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    setStatus("Loi ket noi: ${t.message}")
                }
            })
    }

    private fun loadSearchResults(keyword: String) {
        setStatus("Dang tim kiem \"$keyword\"...")
        RetrofitClient.instance.searchBooks(keyword)
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        val results = response.body().orEmpty()
                        updateBooks(results)
                        if (results.isEmpty()) setStatus("Khong tim thay ket qua cho \"$keyword\"")
                    } else {
                        setStatus("Loi tim kiem (HTTP ${response.code()})")
                    }
                }
                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    setStatus("Loi ket noi: ${t.message}")
                }
            })
    }

    private fun updateBooks(books: List<Book>) {
        allBooks.clear()
        allBooks.addAll(books)
        currentPage = 1
        renderPage()
    }

    private fun renderPage() {
        if (allBooks.isEmpty()) {
            catalogAdapter.submitList(emptyList())
            tvPage.text = "0/0"
            btnPrev.isEnabled = false
            btnNext.isEnabled = false
            tvStatus.visibility = View.VISIBLE
            tvStatus.text = "Khong co sach"
            return
        }

        tvStatus.visibility = View.GONE
        val total = getTotalPages()
        currentPage = currentPage.coerceIn(1, total)
        val start = (currentPage - 1) * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, allBooks.size)
        catalogAdapter.submitList(allBooks.subList(start, end))
        tvPage.text = "$currentPage/$total"
        btnPrev.isEnabled = currentPage > 1
        btnNext.isEnabled = currentPage < total
    }

    private fun getTotalPages() =
        if (allBooks.isEmpty()) 1 else (allBooks.size + PAGE_SIZE - 1) / PAGE_SIZE

    private fun setStatus(msg: String) {
        tvStatus.text = msg
        tvStatus.visibility = View.VISIBLE
    }
}
