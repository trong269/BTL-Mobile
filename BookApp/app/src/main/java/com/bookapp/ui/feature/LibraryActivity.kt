package com.bookapp.ui.feature

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.local.OfflineManager
import com.bookapp.data.local.entities.LocalBook
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LibraryActivity : AppCompatActivity() {

    private lateinit var tvFavoritesCount: TextView
    private lateinit var tvRecentsCount: TextView
    private lateinit var tvOfflineCount: TextView
    private lateinit var tvFavoritesEmpty: TextView
    private lateinit var tvRecentsEmpty: TextView
    private lateinit var tvOfflineEmpty: TextView
    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var recyclerRecents: RecyclerView
    private lateinit var recyclerOffline: RecyclerView

    private lateinit var favoriteAdapter: LibraryBookAdapter
    private lateinit var recentAdapter: LibraryBookAdapter
    private lateinit var offlineAdapter: LibraryBookAdapter
    private lateinit var offlineManager: OfflineManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        bindViews()
        bindActions()
        offlineManager = OfflineManager(this)
    }

    override fun onResume() {
        super.onResume()
        renderData()
    }

    private fun bindViews() {
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount)
        tvRecentsCount = findViewById(R.id.tvRecentsCount)
        tvOfflineCount = findViewById(R.id.tvOfflineCount)
        tvFavoritesEmpty = findViewById(R.id.tvFavoritesEmpty)
        tvRecentsEmpty = findViewById(R.id.tvRecentsEmpty)
        tvOfflineEmpty = findViewById(R.id.tvOfflineEmpty)
        recyclerFavorites = findViewById(R.id.recyclerFavoriteBooks)
        recyclerRecents = findViewById(R.id.recyclerRecentBooks)
        recyclerOffline = findViewById(R.id.recyclerOfflineBooks)

        favoriteAdapter = LibraryBookAdapter("Yêu thích")
        recentAdapter = LibraryBookAdapter("Đọc gần đây")
        offlineAdapter = LibraryBookAdapter("Đã tải")

        recyclerFavorites.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerFavorites.adapter = favoriteAdapter

        recyclerRecents.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerRecents.adapter = recentAdapter

        recyclerOffline.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerOffline.adapter = offlineAdapter
    }

    private fun bindActions() {
        findViewById<ImageButton>(R.id.btnLibraryBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnViewAllFavorites).setOnClickListener {
            startActivity(Intent(this, FavoriteListActivity::class.java))
        }
        findViewById<TextView>(R.id.btnViewAllHistory).setOnClickListener {
            startActivity(Intent(this, ReadingHistoryActivity::class.java))
        }
        findViewById<TextView>(R.id.btnViewAllOffline).setOnClickListener {
            startActivity(Intent(this, OfflineBookListActivity::class.java))
        }
    }

    private fun renderData() {
        val favorites = LibraryStorage.getFavorites(this)
        val recents = LibraryStorage.getRecentReads(this)

        favoriteAdapter.submitList(favorites)
        recentAdapter.submitList(recents)

        lifecycleScope.launch {
            val offlineBooks = offlineManager.getLocalBookList()
                .sortedByDescending { it.downloadedAt }
            val offlineItems = offlineBooks.map { 
                LibraryStorage.LibraryBookItem(it.id, it.title, it.author, it.coverImage, null, it.downloadedAt)
            }
            offlineAdapter.submitList(offlineItems)
            tvOfflineEmpty.visibility = if (offlineItems.isEmpty()) View.VISIBLE else View.GONE
            tvOfflineCount.text = "${offlineItems.size} quyển sách"
        }

        tvFavoritesCount.text = "${favorites.size} quyển sách"
        tvRecentsCount.text = "${recents.size} quyển sách"

        tvFavoritesEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        tvRecentsEmpty.visibility = if (recents.isEmpty()) View.VISIBLE else View.GONE

        val initialTab = intent.getStringExtra(EXTRA_INITIAL_TAB)
        if (initialTab == TAB_FAVORITES) {
            findViewById<View>(R.id.sectionFavorites).post {
                findViewById<View>(R.id.sectionFavorites).requestFocus()
            }
        }
        if (initialTab == TAB_RECENTS) {
            findViewById<View>(R.id.sectionRecents).post {
                findViewById<View>(R.id.sectionRecents).requestFocus()
            }
        }
    }

    companion object {
        const val EXTRA_INITIAL_TAB = "extra_initial_tab"
        const val TAB_FAVORITES = "favorites"
        const val TAB_RECENTS = "recents"
    }
}
