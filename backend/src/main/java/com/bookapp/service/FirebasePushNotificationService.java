package com.bookapp.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushNotificationService {

    public void sendPushNotification(String token, String title, String body) {
        if (token == null || token.isEmpty()) {
            System.err.println("FCM Token is null or empty. Cannot send notification.");
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            System.err.println("Firebase is not initialized. Cannot send push notification.");
            return;
        }

        try {
            System.out.println("Attempting to send FCM notification to token: " + token.substring(0, Math.min(20, token.length())) + "...");

            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent FCM message: " + response);
        } catch (Exception e) {
            System.err.println("Error sending FCM notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
