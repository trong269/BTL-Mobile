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
                Toast.makeText(this, "Ban chua chon sach nao", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Xoa muc da chon")
                .setMessage("Ban co chac muon xoa ${selectedIds.size} muc lich su da chon?")
                .setNegativeButton("Huy", null)
                .setPositiveButton("Xoa") { _, _ ->
                    LibraryStorage.removeRecentByBookIds(this, selectedIds)
                    historyAdapter.clearSelection()
                    syncSelectionUi(0)
                    renderData()
                    Toast.makeText(this, "Da xoa ${selectedIds.size} muc lich su", Toast.LENGTH_SHORT).show()
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
            "Da chon $selectedCount muc"
        } else {
            ""
        }
    }

    private fun showHistoryRangeSelector() {
        val options = arrayOf("1 gio gan nhat", "24 gio gan nhat", "7 ngay gan nhat", "Tat ca lich su")

        AlertDialog.Builder(this)
            .setTitle("Xoa lich su")
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
            .setTitle("Xoa tat ca lich su")
            .setMessage("Ban co chac muon xoa tat ca lich su doc?")
            .setNegativeButton("Huy", null)
            .setPositiveButton("Xoa") { _, _ ->
                LibraryStorage.clearAllRecents(this)
                renderData()
                Toast.makeText(this, "Da xoa toan bo lich su doc", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun confirmDeleteWithin(durationMs: Long, label: String) {
        AlertDialog.Builder(this)
            .setTitle("Xoa lich su")
            .setMessage("Ban co chac muon xoa lich su doc trong $label?")
            .setNegativeButton("Huy", null)
            .setPositiveButton("Xoa") { _, _ ->
                val removed = LibraryStorage.clearRecentsWithinLast(this, durationMs)
                renderData()
                Toast.makeText(this, "Da xoa $removed muc", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
