package com.bookapp.service;

import com.bookapp.model.Notification;
import com.bookapp.model.User;
import com.bookapp.repository.NotificationRepository;
import com.bookapp.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationCronService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FirebasePushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;

    public NotificationCronService(UserRepository userRepository,
                                   EmailService emailService,
                                   FirebasePushNotificationService pushNotificationService,
                                   NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.pushNotificationService = pushNotificationService;
        this.notificationRepository = notificationRepository;
    }

    // Run every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendInactivityNotifications() {
        System.out.println("Running inactivity notification cron job...");
        List<User> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            if (user.getLastActiveAt() != null) {
                long daysInactive = ChronoUnit.DAYS.between(user.getLastActiveAt(), now);

                String title = "";
                String body = "";

                if (daysInactive == 1) {
                    title = "Nhớ sách không bạn ơi? \uD83E\uDD7A";
                    body = "Chỉ một chương thôi cũng giúp duy trì thói quen đọc sách của bạn đấy!";
                } else if (daysInactive == 3) {
                    title = "Đừng làm đứt chuỗi đọc sách! \uD83D\uDD25";
                    body = "Các nhân vật trong sách đang chờ bạn quay lại. Vào đọc tiếp thôi nào!";
                } else if (daysInactive == 7) {
                    title = "Đã 1 tuần trôi qua! \uD83D\uDD70️";
                    body = "Bạn đã không mở ứng dụng một tuần rồi. Hãy dành ra 15 phút hôm nay để đọc sách nhé.";
                } else if (daysInactive == 14) {
                    title = "Chúng mình nhớ bạn! \uD83D\uDCDA";
                    body = "Đừng để thói quen đọc sách phai nhạt. Rất nhiều cuốn sách hay đang chờ bạn khám phá.";
                }

                if (!title.isEmpty()) {
                    // Save to database
                    Notification notification = new Notification(user.getId(), title, body);
                    notificationRepository.save(notification);

                    // Send Push Notification
                    if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                        pushNotificationService.sendPushNotification(user.getFcmToken(), title, body);
                    }
                    
                    // Send Email Notification
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        emailService.sendEmail(user.getEmail(), title, body);
                    }
                }
            }
        }
        System.out.println("Cron job finished.");
    }

    private void broadcastNotification(String title, String body) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            // Save to database
            Notification notification = new Notification(user.getId(), title, body);
            notification.setType("REMINDER");
            notificationRepository.save(notification);

            // Send Push Notification
            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                pushNotificationService.sendPushNotification(user.getFcmToken(), title, body);
            }
        }
    }

    @Scheduled(cron = "0 30 7 * * ?") // 07:30 AM every day
    public void sendMorningGreeting() {
        System.out.println("Running morning notification cron job...");
        broadcastNotification("Chào buổi sáng rực rỡ! ☀️", "Đừng quên nạp năng lượng bằng một vài trang sách hay nhé.");
    }

    @Scheduled(cron = "0 30 12 * * ?") // 12:30 PM every day
    public void sendLunchBreakReading() {
        System.out.println("Running lunch break notification cron job...");
        broadcastNotification("Giờ nghỉ trưa đến rồi! ☕", "Để đôi mắt thư giãn bằng cách đắm chìm vào thế giới sách nào.");
    }

    @Scheduled(cron = "0 30 21 * * ?") // 09:30 PM every day
    public void sendEveningReading() {
        System.out.println("Running evening notification cron job...");
        broadcastNotification("Thời gian hoàn hảo để đọc sách 🌙", "Chọn một cuốn sách yêu thích để khép lại một ngày thật đẹp nhé!");
    }
}
