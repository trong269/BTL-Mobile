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

class LibraryActivity : AppCompatActivity() {

    private lateinit var tvFavoritesCount: TextView
    private lateinit var tvRecentsCount: TextView
    private lateinit var tvFavoritesEmpty: TextView
    private lateinit var tvRecentsEmpty: TextView
    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var recyclerRecents: RecyclerView

    private lateinit var favoriteAdapter: LibraryBookAdapter
    private lateinit var recentAdapter: LibraryBookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        bindViews()
        bindActions()
    }

    override fun onResume() {
        super.onResume()
        renderData()
    }

    private fun bindViews() {
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount)
        tvRecentsCount = findViewById(R.id.tvRecentsCount)
        tvFavoritesEmpty = findViewById(R.id.tvFavoritesEmpty)
        tvRecentsEmpty = findViewById(R.id.tvRecentsEmpty)
        recyclerFavorites = findViewById(R.id.recyclerFavoriteBooks)
        recyclerRecents = findViewById(R.id.recyclerRecentBooks)

        favoriteAdapter = LibraryBookAdapter("Yeu thich")
        recentAdapter = LibraryBookAdapter("Doc gan day")

        recyclerFavorites.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerFavorites.adapter = favoriteAdapter

        recyclerRecents.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerRecents.adapter = recentAdapter
    }

    private fun bindActions() {
        findViewById<ImageButton>(R.id.btnLibraryBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnViewAllFavorites).setOnClickListener {
            startActivity(Intent(this, FavoriteListActivity::class.java))
        }
        findViewById<TextView>(R.id.btnViewAllHistory).setOnClickListener {
            startActivity(Intent(this, ReadingHistoryActivity::class.java))
        }
    }

    private fun renderData() {
        val favorites = LibraryStorage.getFavorites(this)
        val recents = LibraryStorage.getRecentReads(this)

        favoriteAdapter.submitList(favorites)
        recentAdapter.submitList(recents)

        tvFavoritesCount.text = favorites.size.toString()
        tvRecentsCount.text = recents.size.toString()

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
