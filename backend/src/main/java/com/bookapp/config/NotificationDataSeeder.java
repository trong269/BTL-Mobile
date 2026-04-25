package com.bookapp.config;

import com.bookapp.model.Notification;
import com.bookapp.model.User;
import com.bookapp.repository.NotificationRepository;
import com.bookapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationDataSeeder implements CommandLineRunner {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationDataSeeder(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            List<Notification> existingNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            
            if (existingNotifications.isEmpty() || existingNotifications.size() < 3) {
                List<Notification> seeds = new ArrayList<>();
                
                // Welcome
                seeds.add(createNotification(user.getId(), 
                        "Chào mừng đến với BookApp \uD83C\uDF89", 
                        "Khám phá hàng ngàn cuốn sách hay và bắt đầu hành trình đọc sách của bạn ngay hôm nay!", 
                        10));
                        
                // Suggestion
                seeds.add(createNotification(user.getId(), 
                        "Gợi ý sách hôm nay cho bạn \uD83D\uDCD6", 
                        "Cuốn sách 'Đắc Nhân Tâm' đang rất được yêu thích. Dành 15 phút để đọc thử nhé?", 
                        5));
                        
                // Reminder
                seeds.add(createNotification(user.getId(), 
                        "Nhắc nhở đọc sách \u23F0", 
                        "Cú ơi, bạn chưa đọc sách hôm nay. Duy trì thói quen đọc sách rất tốt cho tâm hồn đó!", 
                        2));
                        
                // Update
                seeds.add(createNotification(user.getId(), 
                        "Cập nhật tính năng mới \uD83D\uDE80", 
                        "BookApp vừa cập nhật tính năng đánh dấu trang sách thông minh. Hãy thử ngay!", 
                        1));
                        
                // Promotion
                seeds.add(createNotification(user.getId(), 
                        "Ưu đãi gói Premium \uD83C\uDF1F", 
                        "Chỉ với 50K/tháng, bạn sẽ được trải nghiệm đọc không giới hạn và không có quảng cáo.", 
                        0));

                // Save only what's missing (simplification: if less than 3, just dump all to give them variety)
                notificationRepository.saveAll(seeds);
            }
        }
    }

    private Notification createNotification(String userId, String title, String body, int minusDays) {
        Notification notification = new Notification(userId, title, body);
        notification.setCreatedAt(LocalDateTime.now().minusDays(minusDays).minusHours(minusDays * 2L));
        return notification;
    }
}
