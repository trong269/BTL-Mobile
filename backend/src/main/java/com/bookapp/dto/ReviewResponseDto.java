package com.bookapp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponseDto {
    private String id;
    private String userId;
    private String username;
    private String fullName;
    private String avatar;
    private String bookId;
    private int rating;
    private String review;
    private LocalDateTime createdAt;
}
