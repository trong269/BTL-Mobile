package com.bookapp.dto;

import lombok.Data;
import java.util.List;

@Data
public class SendNotificationRequest {
    private String title;
    private String body;
    private List<String> userIds;
    private boolean sendToAll;
}