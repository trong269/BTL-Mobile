package com.bookapp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentResponseDto {
    private String id;
    private String userId;
    private String username;
    private String fullName;
    private String avatar;
    private String bookId;
    private String content;
    private LocalDateTime createdAt;
}
