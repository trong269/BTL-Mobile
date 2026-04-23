package com.bookapp.ui.book

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.model.Review

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private val items = mutableListOf<Review>()

    fun submitList(newItems: List<Review>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUser: TextView = itemView.findViewById(R.id.tvReviewUser)
        private val tvDate: TextView = itemView.findViewById(R.id.tvReviewDate)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarReview)
        private val tvContent: TextView = itemView.findViewById(R.id.tvReviewContent)

        fun bind(review: Review) {
            tvUser.text = review.userId?.take(8)?.let { "Người dùng #$it" } ?: "Ẩn danh"
            tvDate.text = review.createdAt?.take(10) ?: ""
            ratingBar.rating = review.rating?.toFloat() ?: 0f
            tvContent.text = review.review?.takeIf { it.isNotBlank() } ?: "(Không có nhận xét)"
        }
    }
}
