package com.bookapp.ui.feature

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import java.util.concurrent.TimeUnit

class ReadingHistoryActivity : AppCompatActivity() {

    private lateinit var tvHistoryCount: TextView
    private lateinit var tvHistoryEmpty: TextView
    private lateinit var tvSelectionHint: TextView
    private lateinit var recyclerHistory: RecyclerView
    private lateinit var btnClearHistory: Button
    private lateinit var btnDeleteSelected: Button
    private lateinit var btnClearSelection: Button

    private lateinit var historyAdapter: ReadingHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading_history)

        bindViews()
        bindActions()
        setupRecycler()
    }

    override fun onResume() {
        super.onResume()
        renderData()
    }

    private fun bindViews() {
        tvHistoryCount = findViewById(R.id.tvHistoryCount)
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty)
        tvSelectionHint = findViewById(R.id.tvSelectionHint)
        recyclerHistory = findViewById(R.id.recyclerReadingHistory)
        btnClearHistory = findViewById(R.id.btnClearHistory)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)
        btnClearSelection = findViewById(R.id.btnClearSelection)
    }

    private fun bindActions() {
        findViewById<ImageButton>(R.id.btnHistoryBack).setOnClickListener { finish() }

        btnClearSelection.setOnClickListener {
            historyAdapter.clearSelection()
            syncSelectionUi(0)
        }

        btnDeleteSelected.setOnClickListener {
            val selectedIds = historyAdapter.getSelectedBookIds()
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "Bạn chưa chọn sách nào", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Xóa mục đã chọn")
                .setMessage("Bạn có chắc muốn xóa ${selectedIds.size} mục lịch sử đã chọn?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa") { _, _ ->
                    LibraryStorage.removeRecentByBookIds(this, selectedIds)
                    historyAdapter.clearSelection()
                    syncSelectionUi(0)
                    renderData()
                    Toast.makeText(this, "Đã xóa ${selectedIds.size} mục lịch sử", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        btnClearHistory.setOnClickListener { showHistoryRangeSelector() }
    }

    private fun setupRecycler() {
        historyAdapter = ReadingHistoryAdapter().apply {
            onSelectionChanged = { count -> syncSelectionUi(count) }
        }

        recyclerHistory.layoutManager = LinearLayoutManager(this)
        recyclerHistory.adapter = historyAdapter
    }

    private fun renderData() {
        val items = LibraryStorage.getRecentReads(this)
        historyAdapter.submitList(items)

        tvHistoryCount.text = items.size.toString()
        tvHistoryEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        if (items.isEmpty()) {
            historyAdapter.clearSelection()
            syncSelectionUi(0)
        }
    }

    private fun syncSelectionUi(selectedCount: Int) {
        val hasSelection = selectedCount > 0

        btnDeleteSelected.visibility = if (hasSelection) View.VISIBLE else View.GONE
        btnClearSelection.visibility = if (hasSelection) View.VISIBLE else View.GONE
        btnDeleteSelected.isEnabled = selectedCount > 0

        tvSelectionHint.visibility = if (hasSelection) View.VISIBLE else View.GONE
        tvSelectionHint.text = if (hasSelection) {
            "Đã chọn $selectedCount mục"
        } else {
            ""
        }
    }

    private fun showHistoryRangeSelector() {
        val options = arrayOf("1 giờ gần nhất", "24 giờ gần nhất", "7 ngày gần nhất", "Tất cả lịch sử")

        AlertDialog.Builder(this)
            .setTitle("Xóa lịch sử")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmDeleteWithin(TimeUnit.HOURS.toMillis(1), options[0])
                    1 -> confirmDeleteWithin(TimeUnit.HOURS.toMillis(24), options[1])
                    2 -> confirmDeleteWithin(TimeUnit.DAYS.toMillis(7), options[2])
                    3 -> confirmDeleteAll()
                }
            }
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("Xóa tất cả lịch sử")
            .setMessage("Bạn có chắc muốn xóa tất cả lịch sử đọc?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                LibraryStorage.clearAllRecents(this)
                renderData()
                Toast.makeText(this, "Đã xóa toàn bộ lịch sử đọc", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun confirmDeleteWithin(durationMs: Long, label: String) {
        AlertDialog.Builder(this)
            .setTitle("Xóa lịch sử")
            .setMessage("Bạn có chắc muốn xóa lịch sử đọc trong $label?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                val removed = LibraryStorage.clearRecentsWithinLast(this, durationMs)
                renderData()
                Toast.makeText(this, "Đã xóa $removed mục", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
