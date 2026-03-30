package com.bookapp.ui.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.model.Book
import com.bookapp.ui.book.BookDetailActivity
import com.bumptech.glide.Glide

class BookAdapter : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val items = mutableListOf<Book>()

    fun submitList(newItems: List<Book>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivBookCover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvBookTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvBookAuthor)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvBookMeta)

        fun bind(book: Book) {
            tvTitle.text = book.title?.takeIf { it.isNotBlank() } ?: "Chua co tieu de"
            tvAuthor.text = book.author?.takeIf { it.isNotBlank() } ?: "Khong ro tac gia"

            val ratingText = book.avgRating?.let { String.format("%.1f", it) } ?: "N/A"
            val viewsText = book.views ?: 0
            tvMeta.text = "★ $ratingText  |  ${viewsText} luot xem"

            // Load ảnh bìa với Glide
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
                val context = itemView.context
                val intent = Intent(context, BookDetailActivity::class.java).apply {
                    putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.id)
                    putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, book.title)
                }
                context.startActivity(intent)
            }
        }
    }
}
