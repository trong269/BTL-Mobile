package com.bookapp.ui.feature

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.util.concurrent.TimeUnit

class LibraryBookAdapter(
    private val badgeText: String
) : RecyclerView.Adapter<LibraryBookAdapter.LibraryBookViewHolder>() {

    private val items = mutableListOf<LibraryStorage.LibraryBookItem>()

    fun submitList(newItems: List<LibraryStorage.LibraryBookItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryBookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_library_book, parent, false)
        return LibraryBookViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryBookViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class LibraryBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivLibraryCover)
        private val tvCoverFallback: TextView = itemView.findViewById(R.id.tvLibraryCoverFallback)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvLibraryTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvLibraryAuthor)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvLibraryMeta)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvLibraryBadge)

        fun bind(item: LibraryStorage.LibraryBookItem) {
            tvTitle.text = item.title
            tvAuthor.text = item.author
            tvBadge.text = badgeText
            tvMeta.text = formatTime(item.updatedAt)

            bindCover(item.coverImage)

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, BookDetailActivity::class.java).apply {
                    putExtra(BookDetailActivity.EXTRA_BOOK_ID, item.bookId)
                    putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, item.title)
                }
                itemView.context.startActivity(intent)
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

    private fun formatTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Vừa cập nhật"

        val now = System.currentTimeMillis()
        val diff = (now - timestamp).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

        return when {
            minutes < 1 -> "Vừa cập nhật"
            minutes < 60 -> "$minutes phut truoc"
            minutes < 24 * 60 -> "${minutes / 60} gio truoc"
            minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} ngay truoc"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
