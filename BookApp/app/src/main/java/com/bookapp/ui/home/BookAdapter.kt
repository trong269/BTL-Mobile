package com.bookapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.model.Book

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
        private val tvTitle: TextView = itemView.findViewById(R.id.tvBookTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvBookAuthor)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvBookMeta)

        fun bind(book: Book) {
            tvTitle.text = book.title?.takeIf { it.isNotBlank() } ?: "Chua co tieu de"
            tvAuthor.text = book.author?.takeIf { it.isNotBlank() } ?: "Khong ro tac gia"

            val ratingText = book.avgRating?.let { String.format("%.1f", it) } ?: "N/A"
            val viewsText = book.views ?: 0
            tvMeta.text = "Danh gia: $ratingText | Luot xem: $viewsText"
        }
    }
}
