package com.bookapp.ui.home

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.BuildConfig
import com.bookapp.R
import com.bookapp.data.model.Book
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class BookAdapter(
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

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

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgBookCover: ImageView = itemView.findViewById(R.id.imgBookCover)
        private val tvBookCoverFallback: TextView = itemView.findViewById(R.id.tvBookCoverFallback)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvBookTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvBookAuthor)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvBookMeta)

        fun bind(book: Book) {
            bindCover(book.coverImage)
            tvTitle.text = book.title?.takeIf { it.isNotBlank() } ?: "Chua co tieu de"
            tvAuthor.text = book.author?.takeIf { it.isNotBlank() } ?: "Khong ro tac gia"

            val ratingText = book.avgRating?.let { String.format("%.1f", it) } ?: "N/A"
            val viewsText = book.views ?: 0
            tvMeta.text = "Danh gia: $ratingText | Luot xem: $viewsText"

            itemView.setOnClickListener {
                onItemClick(book)
            }
        }

        private fun bindCover(coverImage: String?) {
            val imageUrl = normalizeCoverUrl(coverImage)
            if (imageUrl == null) {
                imgBookCover.setImageDrawable(null)
                tvBookCoverFallback.visibility = View.VISIBLE
                return
            }

            tvBookCoverFallback.visibility = View.GONE
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
                        tvBookCoverFallback.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        tvBookCoverFallback.visibility = View.GONE
                        return false
                    }
                })
                .into(imgBookCover)
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
}
