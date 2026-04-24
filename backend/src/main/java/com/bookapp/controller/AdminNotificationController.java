package com.bookapp.controller;

import com.bookapp.dto.SendNotificationRequest;
import com.bookapp.service.AdminNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    public AdminNotificationController(AdminNotificationService adminNotificationService) {
        this.adminNotificationService = adminNotificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody SendNotificationRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Title is required");
        }

        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Body is required");
        }

        if (!request.isSendToAll() && (request.getUserIds() == null || request.getUserIds().isEmpty())) {
            return ResponseEntity.badRequest().body("User IDs are required when not sending to all");
        }

        adminNotificationService.sendNotification(request);
        return ResponseEntity.ok().body("Notification sent successfully");
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAllAdminNotifications() {
        long deletedCount = adminNotificationService.deleteAllAdminNotifications();
        return ResponseEntity.ok().body("Deleted " + deletedCount + " admin notifications");
    }
}
