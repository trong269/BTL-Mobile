package com.bookapp.ui.book

import android.content.Intent
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.model.Book
import com.bumptech.glide.Glide

class BookCatalogAdapter(
    private val categoryNameResolver: (String?) -> String
) : RecyclerView.Adapter<BookCatalogAdapter.CatalogViewHolder>() {

    private val items = mutableListOf<Book>()
    private var lastOpenBookDetailMs = 0L

    fun submitList(newItems: List<Book>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<Book> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_catalog, parent, false)
        return CatalogViewHolder(view)
    }

    override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CatalogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivCatalogCover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvCatalogTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvCatalogAuthor)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCatalogCategory)
        private val tvRating: TextView = itemView.findViewById(R.id.tvCatalogRating)
        private val tvChapters: TextView = itemView.findViewById(R.id.tvCatalogChapters)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvCatalogDescription)

        fun bind(book: Book) {
            tvTitle.text = book.title ?: "Chưa có tiêu đề"
            tvAuthor.text = "Tác giả: ${book.author ?: "Không rõ"}"
            tvCategory.text = categoryNameResolver(book.categoryId)
            tvRating.text = book.avgRating?.let { String.format("%.1f", it) } ?: "N/A"
            tvChapters.text = "${book.totalChapters ?: 0} chuong"
            
            // Decode HTML entities and strip HTML tags from description
            val description = book.description ?: ""
            tvDescription.text = decodeHtmlDescription(description)

            // Load ảnh bìa
            if (!book.coverImage.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(book.coverImage)
                    .placeholder(R.drawable.book_cover_placeholder)
                    .error(R.drawable.book_cover_placeholder)
                    .centerCrop()
                    .into(ivCover)
            } else {
                ivCover.setImageResource(R.drawable.book_cover_placeholder)
            }

            // Click → BookDetailActivity
            itemView.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastOpenBookDetailMs < 600L) {
                    return@setOnClickListener
                }

                val context = itemView.context
                val id = book.id?.trim()
                if (id.isNullOrEmpty()) {
                    Toast.makeText(context, "Sách này chưa có ID hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lastOpenBookDetailMs = now
                val intent = Intent(context, BookDetailActivity::class.java).apply {
                    putExtra(BookDetailActivity.EXTRA_BOOK_ID, id)
                    putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, book.title)
                }
                context.startActivity(intent)
            }
        }

        private fun decodeHtmlDescription(html: String): CharSequence {
            // Decode HTML entities first
            val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html)
            }
            
            // Strip HTML tags (if any remain)
            val text = decoded.toString()
            return text.replace("<[^>]*>".toRegex(), "")  // Remove HTML tags
                .replace("&[a-zA-Z]+;".toRegex(), "")     // Remove remaining HTML entities
                .replace("&#\\d+;".toRegex(), "")         // Remove numeric entities
                .replace("&#x[0-9a-fA-F]+;".toRegex(), "")  // Remove hex entities
                .trim()
        }
    }
}
