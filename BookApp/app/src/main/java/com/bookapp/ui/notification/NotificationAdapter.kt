package com.bookapp.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.model.Notification
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvBody: TextView = view.findViewById(R.id.tvBody)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val viewUnreadDot: View = view.findViewById(R.id.viewUnreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.tvTitle.text = notification.title
        holder.tvBody.text = notification.body
        holder.tvTime.text = formatTime(notification.createdAt)
        
        holder.viewUnreadDot.visibility = if (notification.read) View.GONE else View.VISIBLE
        
        holder.itemView.setOnClickListener {
            onItemClick(notification)
        }
    }

    override fun getItemCount() = notifications.size
    
    private fun formatTime(timeStr: String): String {
        try {
            val dateTime = LocalDateTime.parse(timeStr)
            val now = LocalDateTime.now()
            
            val days = ChronoUnit.DAYS.between(dateTime, now)
            val hours = ChronoUnit.HOURS.between(dateTime, now)
            val minutes = ChronoUnit.MINUTES.between(dateTime, now)
            
            return when {
                days > 0 -> "$days ngày trước"
                hours > 0 -> "$hours giờ trước"
                minutes > 0 -> "$minutes phút trước"
                else -> "Vừa xong"
            }
        } catch (e: Exception) {
            return timeStr
        }
    }
}
