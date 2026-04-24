package com.bookapp.ui.notification

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookapp.R
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.model.Notification
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: NotificationAdapter
    private val notificationsList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        recyclerNotifications = findViewById(R.id.recyclerNotifications)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        adapter = NotificationAdapter(notificationsList) { notification ->
            if (!notification.read) {
                markAsRead(notification)
            }
            showNotificationDetail(notification)
        }

        recyclerNotifications.layoutManager = LinearLayoutManager(this)
        recyclerNotifications.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val prefs = getSharedPreferences("BookAppPrefs", MODE_PRIVATE)
        val userId = prefs.getString("userId", null)
        
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem thông báo", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        RetrofitClient.instance.getUserNotifications(userId)
            .enqueue(object : Callback<List<Notification>> {
                override fun onResponse(
                    call: Call<List<Notification>>,
                    response: Response<List<Notification>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        notificationsList.clear()
                        notificationsList.addAll(response.body()!!)
                        adapter.notifyDataSetChanged()

                        tvEmptyState.visibility = if (notificationsList.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Toast.makeText(this@NotificationActivity, "Lỗi khi lấy thông báo", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                    Toast.makeText(this@NotificationActivity, "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun markAsRead(notification: Notification) {
        RetrofitClient.instance.markNotificationAsRead(notification.id)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        notification.read = true
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Ignore background failure
                }
            })
    }

    private fun showNotificationDetail(notification: Notification) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(notification.title)
            .setMessage(notification.body)
            .setPositiveButton("Đóng", null)
            .show()
    }
}
