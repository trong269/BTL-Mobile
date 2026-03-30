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

class TopBookAdapter(
    private var metaPrefix: String,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<TopBookAdapter.TopBookViewHolder>() {

    private val items = mutableListOf<Book>()

    fun submitList(newItems: List<Book>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun submitList(newItems: List<Book>, newMetaPrefix: String) {
        metaPrefix = newMetaPrefix
        submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopBookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_book, parent, false)
        return TopBookViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopBookViewHolder, position: Int) {
        holder.bind(items[position], position + 1, metaPrefix)
    }

    override fun getItemCount(): Int = items.size

    inner class TopBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTopRank: TextView = itemView.findViewById(R.id.tvTopRank)
        private val imgTopBookCover: ImageView = itemView.findViewById(R.id.imgTopBookCover)
        private val tvTopBookCoverFallback: TextView = itemView.findViewById(R.id.tvTopBookCoverFallback)
        private val tvTopBookTitle: TextView = itemView.findViewById(R.id.tvTopBookTitle)
        private val tvTopBookAuthor: TextView = itemView.findViewById(R.id.tvTopBookAuthor)
        private val tvTopBookMeta: TextView = itemView.findViewById(R.id.tvTopBookMeta)

        fun bind(book: Book, rank: Int, metaPrefix: String) {
            val ratingText = book.avgRating?.let { String.format("%.1f", it) } ?: "N/A"
            val viewsText = book.views ?: 0

            bindCover(book.coverImage)
            tvTopRank.text = "Top #$rank"
            tvTopBookTitle.text = book.title?.takeIf { it.isNotBlank() } ?: "Chua co tieu de"
            tvTopBookAuthor.text = book.author?.takeIf { it.isNotBlank() } ?: "Khong ro tac gia"

            val metaParts = mutableListOf<String>()
            if (viewsText > 0) {
                metaParts.add("$viewsText xem")
            }
            if (ratingText != "N/A") {
                metaParts.add("$ratingText sao")
            }

            if (metaParts.isEmpty()) {
                tvTopBookMeta.visibility = View.GONE
            } else {
                tvTopBookMeta.visibility = View.VISIBLE
                tvTopBookMeta.text = metaParts.joinToString(" | ")
            }

            itemView.setOnClickListener {
                onItemClick(book)
            }
        }

        private fun bindCover(coverImage: String?) {
            val imageUrl = normalizeCoverUrl(coverImage)
            if (imageUrl == null) {
                imgTopBookCover.setImageDrawable(null)
                tvTopBookCoverFallback.visibility = View.VISIBLE
                return
            }

            tvTopBookCoverFallback.visibility = View.GONE
            Glide.with(itemView)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        tvTopBookCoverFallback.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        tvTopBookCoverFallback.visibility = View.GONE
                        return false
                    }
                })
                .into(imgTopBookCover)
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
