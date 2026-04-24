package com.bookapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bookapp.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        android.util.Log.d("FCM", "Message received from: ${remoteMessage.from}")
        android.util.Log.d("FCM", "Message data: ${remoteMessage.data}")
        android.util.Log.d("FCM", "Message notification: ${remoteMessage.notification}")

        // Always show notification, even when app is in foreground
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Thông báo"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""

        android.util.Log.d("FCM", "Showing notification: title=$title, body=$body")
        sendNotification(title, body)
    }

    override fun onNewToken(token: String) {
        // Send token to backend if needed
        android.util.Log.d("FCM", "New FCM token: $token")
        super.onNewToken(token)
    }

    private fun sendNotification(title: String, messageBody: String) {
        android.util.Log.d("FCM", "sendNotification called: title=$title, body=$messageBody")

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "BookApp_Notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Book App Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.enableVibration(true)
            channel.enableLights(true)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
        android.util.Log.d("FCM", "Notification displayed with ID: $notificationId")
    }
}
