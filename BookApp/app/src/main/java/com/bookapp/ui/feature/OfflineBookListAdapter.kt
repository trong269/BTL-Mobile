package com.bookapp.ui.feature

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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

class OfflineBookListAdapter(
    private val onDeleteClick: (LibraryStorage.LibraryBookItem) -> Unit
) : RecyclerView.Adapter<OfflineBookListAdapter.ViewHolder>() {

    private val items = mutableListOf<LibraryStorage.LibraryBookItem>()

    fun submitList(newItems: List<LibraryStorage.LibraryBookItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offline_book_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivOfflineCover)
        private val tvCoverFallback: TextView = itemView.findViewById(R.id.tvOfflineCoverFallback)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvOfflineTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvOfflineAuthor)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteOffline)

        fun bind(item: LibraryStorage.LibraryBookItem) {
            tvTitle.text = item.title
            tvAuthor.text = item.author

            val imageUrl = normalizeCoverUrl(item.coverImage)
            if (imageUrl == null) {
                ivCover.setImageDrawable(null)
                tvCoverFallback.visibility = View.VISIBLE
            } else {
                tvCoverFallback.visibility = View.GONE
                Glide.with(itemView)
                    .load(imageUrl)
                    .placeholder(R.drawable.book_cover_placeholder)
                    .error(R.drawable.book_cover_placeholder)
                    .into(ivCover)
            }

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, BookDetailActivity::class.java).apply {
                    putExtra(BookDetailActivity.EXTRA_BOOK_ID, item.bookId)
                    putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, item.title)
                }
                itemView.context.startActivity(intent)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }

        private fun normalizeCoverUrl(raw: String?): String? {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) return null
            return if (value.startsWith("http")) value 
            else BuildConfig.BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
        }
    }
}
