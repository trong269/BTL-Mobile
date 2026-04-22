package com.bookapp.ui.book

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.model.Comment

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val items = mutableListOf<Comment>()

    fun submitList(newItems: List<Comment>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUser: TextView = itemView.findViewById(R.id.tvCommentUser)
        private val tvDate: TextView = itemView.findViewById(R.id.tvCommentDate)
        private val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)

        fun bind(comment: Comment) {
            tvUser.text = comment.userId?.take(8)?.let { "Người dùng #$it" } ?: "Ẩn danh"
            tvDate.text = comment.createdAt?.take(10) ?: ""
            tvContent.text = comment.content ?: ""
        }
    }
}
