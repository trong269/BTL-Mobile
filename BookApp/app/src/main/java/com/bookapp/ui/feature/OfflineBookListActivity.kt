package com.bookapp.ui.feature

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.local.OfflineManager
import android.widget.Toast
import kotlinx.coroutines.launch

class OfflineBookListActivity : AppCompatActivity() {

    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var recyclerOffline: RecyclerView
    private lateinit var offlineAdapter: OfflineBookListAdapter
    private lateinit var offlineManager: OfflineManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_book_list)

        bindViews()
        bindActions()
        setupRecycler()
        offlineManager = OfflineManager(this)
    }

    override fun onResume() {
        super.onResume()
        renderData()
    }

    private fun bindViews() {
        tvCount = findViewById(R.id.tvOfflineListCount)
        tvEmpty = findViewById(R.id.tvOfflineListEmpty)
        recyclerOffline = findViewById(R.id.recyclerOfflineList)
    }

    private fun bindActions() {
        findViewById<ImageButton>(R.id.btnOfflineListBack).setOnClickListener { finish() }
    }

    private fun setupRecycler() {
        offlineAdapter = OfflineBookListAdapter { item ->
            AlertDialog.Builder(this)
                .setTitle("Xóa sách đã tải")
                .setMessage("Bạn có chắc chắn muốn xóa bản offline của cuốn '${item.title}' không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa") { _, _ ->
                    lifecycleScope.launch {
                        offlineManager.deleteDownloadedBook(item.bookId)
                        renderData()
                        Toast.makeText(this@OfflineBookListActivity, "Đã xóa bản offline", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
        recyclerOffline.layoutManager = LinearLayoutManager(this)
        recyclerOffline.adapter = offlineAdapter
    }

    private fun renderData() {
        lifecycleScope.launch {
            val offlineBooks = offlineManager.getLocalBookList()
                .sortedByDescending { it.downloadedAt }
            
            val offlineItems = offlineBooks.map { 
                LibraryStorage.LibraryBookItem(
                    it.id, it.title, it.author, it.coverImage, null, it.downloadedAt
                )
            }
            
            offlineAdapter.submitList(offlineItems)
            tvCount.text = offlineItems.size.toString()
            tvEmpty.visibility = if (offlineItems.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
