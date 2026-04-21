package com.bookapp.ui.feature

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

class FavoriteListAdapter(
    private val onRemoveFavorite: (LibraryStorage.LibraryBookItem) -> Unit
) : RecyclerView.Adapter<FavoriteListAdapter.FavoriteViewHolder>() {

    private val items = mutableListOf<LibraryStorage.LibraryBookItem>()

    fun submitList(newItems: List<LibraryStorage.LibraryBookItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_book, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivFavoriteCover)
        private val tvFallback: TextView = itemView.findViewById(R.id.tvFavoriteCoverFallback)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvFavoriteTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvFavoriteAuthor)
        private val btnUnfavorite: Button = itemView.findViewById(R.id.btnUnfavorite)

        fun bind(item: LibraryStorage.LibraryBookItem) {
            tvTitle.text = item.title
            tvAuthor.text = item.author
            bindCover(item.coverImage)

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, BookDetailActivity::class.java).apply {
                    putExtra(BookDetailActivity.EXTRA_BOOK_ID, item.bookId)
                    putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, item.title)
                }
                itemView.context.startActivity(intent)
            }

            btnUnfavorite.setOnClickListener {
                onRemoveFavorite(item)
            }
        }

        private fun bindCover(coverImage: String?) {
            val imageUrl = normalizeCoverUrl(coverImage)
            if (imageUrl == null) {
                ivCover.setImageDrawable(null)
                tvFallback.visibility = View.VISIBLE
                return
            }

            tvFallback.visibility = View.GONE
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
                        tvFallback.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        tvFallback.visibility = View.GONE
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
}
