package com.bookapp.ui.book

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
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
    private val allBooksOriginal = mutableListOf<Book>()  // Lưu danh sách gốc để hiển thị filter options
    private val categories = mutableListOf<Category>()
    private var selectedCategoryId: String? = null  // null = Tất cả
    private var currentPage = 1
    private var currentMode = "all"
    private var searchQuery = ""
    
    // Filter states
    private val selectedCategoriesForFilter = mutableSetOf<String>()  // Để lọc, riêng biệt với selectedCategoryId
    private var minRating = 0.0  // 0.0 = hiển thị tất cả
    private var selectedYear: Int? = null  // null = hiển thị tất cả năm

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

        // Filter button - show dialog
        val btnToggleFilter: Button = findViewById(R.id.btnToggleFilter)
        btnToggleFilter.setOnClickListener {
            showFilterDialog()
        }
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
        addTab("Tất cả", null)
        // Tab "Nổi bật"
        addTabMode("Nổi bật", "featured")
        // Tab "Mới nhất"
        addTabMode("Mới nhất", "new")
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
        setStatus("Đang tải...")
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
                    setStatus("Lỗi tải sách (HTTP ${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                setStatus("Lỗi kết nối: ${t.message}")
            }
        })
    }

    private fun loadBooksByCategory(categoryId: String) {
        setStatus("Đang tải...")
        RetrofitClient.instance.getBooksByCategory(categoryId)
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        updateBooks(response.body().orEmpty())
                    } else {
                        setStatus("Lỗi tải sách (HTTP ${response.code()})")
                    }
                }
                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    setStatus("Lỗi kết nối: ${t.message}")
                }
            })
    }

    private fun loadSearchResults(keyword: String) {
        setStatus("Đang tìm kiếm \"$keyword\"...")
        RetrofitClient.instance.searchBooks(keyword)
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        val results = response.body().orEmpty()
                        updateBooks(results)
                        if (results.isEmpty()) setStatus("Không tìm thấy kết quả cho \"$keyword\"")
                    } else {
                        setStatus("Lỗi tìm kiếm (HTTP ${response.code()})")
                    }
                }
                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    setStatus("Lỗi kết nối: ${t.message}")
                }
            })
    }

    private fun updateBooks(books: List<Book>) {
        allBooksOriginal.clear()
        allBooksOriginal.addAll(books)
        allBooks.clear()
        allBooks.addAll(books)
        currentPage = 1
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = allBooksOriginal.filter { book ->
            // Filter by category
            val matchesCategory = if (selectedCategoriesForFilter.isEmpty()) {
                true
            } else {
                book.categoryId != null && selectedCategoriesForFilter.contains(book.categoryId)
            }
            
            // Filter by rating
            val matchesRating = (book.avgRating ?: 0.0) >= minRating
            
            // Filter by year
            val matchesYear = if (selectedYear == null) {
                true
            } else {
                val publishYear = extractYear(book)
                publishYear == selectedYear
            }
            
            matchesCategory && matchesRating && matchesYear
        }
        
        allBooks.clear()
        allBooks.addAll(filtered)
        renderPage()
    }
    
    private fun extractYear(book: Book): Int? {
        return try {
            val dateStr = book.createdAt?.substring(0, 4)
            dateStr?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun renderPage() {
        if (allBooks.isEmpty()) {
            catalogAdapter.submitList(emptyList())
            tvPage.text = "0/0"
            btnPrev.isEnabled = false
            btnNext.isEnabled = false
            tvStatus.visibility = View.VISIBLE
            tvStatus.text = "Không có sách"
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

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_filter, null)
        
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Bộ lọc sách")
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.home_card_bg)
        
        // Set dialog size
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        // Populate filter options in dialog
        populateDialogFilters(dialogView)
        
        // Apply button
        val btnApply: Button = dialogView.findViewById(R.id.btnDialogApplyFilter)
        btnApply.setOnClickListener {
            applyFilters()
            dialog.dismiss()
        }
        
        // Clear button
        val btnClear: Button = dialogView.findViewById(R.id.btnDialogClearFilters)
        btnClear.setOnClickListener {
            selectedCategoriesForFilter.clear()
            minRating = 0.0
            selectedYear = null
            populateDialogFilters(dialogView)
        }
        
        dialog.show()
    }

    private fun populateDialogFilters(dialogView: android.view.View) {
        try {
            buildDialogCategoryFilter(dialogView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            buildDialogRatingFilter(dialogView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            buildDialogYearFilter(dialogView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildDialogCategoryFilter(dialogView: android.view.View) {
        val llCategoryFilter: LinearLayout = dialogView.findViewById(R.id.llDialogCategoryFilter)
        llCategoryFilter.removeAllViews()

        // Filter categories with valid IDs
        val validCategories = categories.filter { it.id != null && it.id.isNotBlank() }
        
        if (validCategories.isEmpty()) {
            llCategoryFilter.visibility = View.GONE
            return
        }

        llCategoryFilter.visibility = View.VISIBLE
        validCategories.forEach { category ->
            val categoryId = category.id!!  // Safe to use !! now
            val btn = Button(this).apply {
                text = category.name ?: ""
                textSize = 11f
                isAllCaps = false
                val isSelected = selectedCategoriesForFilter.contains(categoryId)
                setBackgroundResource(if (isSelected) R.drawable.chip_selected_bg else R.drawable.chip_unselected_bg)
                setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF23408E.toInt())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 6 }
                layoutParams = lp
                setPadding(28, 8, 28, 8)
                setOnClickListener {
                    if (selectedCategoriesForFilter.contains(categoryId)) {
                        selectedCategoriesForFilter.remove(categoryId)
                    } else {
                        selectedCategoriesForFilter.add(categoryId)
                    }
                    buildDialogCategoryFilter(dialogView)
                }
            }
            llCategoryFilter.addView(btn)
        }
    }

    private fun buildDialogRatingFilter(dialogView: android.view.View) {
        val llRatingFilter: LinearLayout = dialogView.findViewById(R.id.llDialogRatingFilter)
        llRatingFilter.removeAllViews()

        val ratings = listOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0)
        val ratingLabels = listOf("All", "☆", "★☆", "★★☆", "★★★☆", "★★★★☆")

        // Luôn hiển thị rating filter
        ratings.zip(ratingLabels).forEach { (rating, label) ->
            val btn = Button(this).apply {
                text = label
                textSize = 11f
                isAllCaps = false
                val isSelected = minRating == rating
                setBackgroundResource(if (isSelected) R.drawable.chip_selected_bg else R.drawable.chip_unselected_bg)
                setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF23408E.toInt())
                val lp = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply { marginEnd = 6 }
                layoutParams = lp
                setPadding(16, 8, 16, 8)
                setOnClickListener {
                    minRating = rating
                    buildDialogRatingFilter(dialogView)
                }
            }
            llRatingFilter.addView(btn)
        }
    }

    private fun buildDialogYearFilter(dialogView: android.view.View) {
        val spinnerYear: Spinner = dialogView.findViewById(R.id.spinnerDialogYearFilter)

        // Collect all unique years from original books and sort them
        val uniqueYears = mutableSetOf<Int>()
        allBooksOriginal.forEach { book ->
            val year = extractYear(book)
            if (year != null) uniqueYears.add(year)
        }

        if (uniqueYears.isEmpty()) {
            spinnerYear.visibility = View.GONE
            return
        }

        spinnerYear.visibility = View.VISIBLE
        
        // Create year list with "Tất cả" option first
        val yearList = mutableListOf("Tất cả")
        yearList.addAll(uniqueYears.sorted().reversed().map { it.toString() })

        // Create adapter and set to spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, yearList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = adapter

        // Set selected item based on current selectedYear
        val selectedPosition = if (selectedYear == null) {
            0  // "Tất cả" position
        } else {
            yearList.indexOf(selectedYear.toString())
        }
        spinnerYear.setSelection(selectedPosition)

        // Custom OnItemSelectedListener to update selectedYear
        spinnerYear.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedYear = if (position == 0) null else yearList[position].toIntOrNull()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
}
