package com.bookapp.ui.feature

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.BuildConfig
import com.bookapp.R
import com.bookapp.ui.book.BookDetailActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReadingHistoryAdapter : RecyclerView.Adapter<ReadingHistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<LibraryStorage.LibraryBookItem>()
    private val selectedIds = linkedSetOf<String>()

    var onSelectionChanged: ((Int) -> Unit)? = null

    fun submitList(newItems: List<LibraryStorage.LibraryBookItem>) {
        items.clear()
        items.addAll(newItems)
        selectedIds.retainAll(newItems.map { it.bookId }.toSet())
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedIds.size)
    }

    fun clearSelection() {
        selectedIds.clear()
        onSelectionChanged?.invoke(0)
        notifyDataSetChanged()
    }

    fun getSelectedBookIds(): Set<String> = selectedIds.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reading_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivHistoryCover)
        private val tvCoverFallback: TextView = itemView.findViewById(R.id.tvHistoryCoverFallback)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvHistoryTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvHistoryAuthor)
        private val tvTime: TextView = itemView.findViewById(R.id.tvHistoryTime)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbHistorySelect)

        fun bind(item: LibraryStorage.LibraryBookItem) {
            val isChecked = selectedIds.contains(item.bookId)

            tvTitle.text = item.title
            tvAuthor.text = item.author
            tvTime.text = formatTimestamp(item.updatedAt)
            cbSelect.visibility = View.VISIBLE

            bindCover(item.coverImage)

            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = isChecked
            cbSelect.setOnCheckedChangeListener { _, checked ->
                updateSelection(item.bookId, checked)
            }

            itemView.setOnClickListener {
                if (selectedIds.isEmpty()) {
                    val intent = Intent(itemView.context, BookDetailActivity::class.java).apply {
                        putExtra(BookDetailActivity.EXTRA_BOOK_ID, item.bookId)
                        putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, item.title)
                    }
                    itemView.context.startActivity(intent)
                } else {
                    val next = !selectedIds.contains(item.bookId)
                    updateSelection(item.bookId, next)
                    cbSelect.isChecked = next
                }
            }
        }

        private fun bindCover(coverImage: String?) {
            val imageUrl = normalizeCoverUrl(coverImage)
            if (imageUrl == null) {
                ivCover.setImageDrawable(null)
                tvCoverFallback.visibility = View.VISIBLE
                return
            }

            tvCoverFallback.visibility = View.GONE
            Glide.with(itemView)
                .load(imageUrl)
                .placeholder(R.drawable.book_cover_placeholder)
                .error(R.drawable.book_cover_placeholder)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        tvCoverFallback.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        tvCoverFallback.visibility = View.GONE
                        return false
                    }
                })
                .into(ivCover)
        }

        private fun updateSelection(bookId: String, selected: Boolean) {
            if (selected) {
                selectedIds.add(bookId)
            } else {
                selectedIds.remove(bookId)
            }
            onSelectionChanged?.invoke(selectedIds.size)
        }
    }

    private fun normalizeCoverUrl(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null

        return if (value.startsWith("http://") || value.startsWith("https://")) {
            value
        } else {
            BuildConfig.BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "Không rõ thời gian"
        val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
        return "Đọc lúc ${sdf.format(Date(timestamp))}"
    }
}
