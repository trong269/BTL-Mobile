package com.bookapp.ui.feature

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R

class FavoriteListActivity : AppCompatActivity() {

    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var favoriteListAdapter: FavoriteListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_list)

        bindViews()
        bindActions()
        setupRecycler()
    }

    override fun onResume() {
        super.onResume()
        renderData()
    }

    private fun bindViews() {
        tvCount = findViewById(R.id.tvFavoriteListCount)
        tvEmpty = findViewById(R.id.tvFavoriteListEmpty)
        recyclerFavorites = findViewById(R.id.recyclerFavoriteList)
    }

    private fun bindActions() {
        findViewById<ImageButton>(R.id.btnFavoriteListBack).setOnClickListener { finish() }
    }

    private fun setupRecycler() {
        favoriteListAdapter = FavoriteListAdapter { item ->
            AlertDialog.Builder(this)
                .setTitle("Bo yeu thich")
                .setMessage("Bo '${item.title}' khoi danh sach yeu thich?")
                .setNegativeButton("Huy", null)
                .setPositiveButton("Bo") { _, _ ->
                    LibraryStorage.removeFavorite(this, item.bookId)
                    renderData()
                    Toast.makeText(this, "Da bo yeu thich", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        recyclerFavorites.layoutManager = LinearLayoutManager(this)
        recyclerFavorites.adapter = favoriteListAdapter
    }

    private fun renderData() {
        val items = LibraryStorage.getFavorites(this)
        favoriteListAdapter.submitList(items)
        tvCount.text = items.size.toString()
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
