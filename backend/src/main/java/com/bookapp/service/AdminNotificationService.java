package com.bookapp.service;

import com.bookapp.dto.SendNotificationRequest;
import com.bookapp.model.Notification;
import com.bookapp.model.User;
import com.bookapp.repository.NotificationRepository;
import com.bookapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminNotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FirebasePushNotificationService fcmService;

    public AdminNotificationService(
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            FirebasePushNotificationService fcmService) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.fcmService = fcmService;
    }

    public void sendNotification(SendNotificationRequest request) {
        List<User> targetUsers;

        if (request.isSendToAll()) {
            targetUsers = userRepository.findAll();
        } else {
            targetUsers = userRepository.findAllById(request.getUserIds());
        }

        for (User user : targetUsers) {
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setTitle(request.getTitle());
            notification.setBody(request.getBody());
            notification.setType("ADMIN");
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);

            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                fcmService.sendPushNotification(user.getFcmToken(), request.getTitle(), request.getBody());
            }
        }
    }

    public long deleteAllAdminNotifications() {
        List<Notification> adminNotifications = notificationRepository.findByType("ADMIN");
        notificationRepository.deleteAll(adminNotifications);
        return adminNotifications.size();
    }
}