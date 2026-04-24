package com.bookapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reviews")
public class Review {

    @Id
    private String id;

    private String userId;
    private String userName;
    private String bookId;

    private int rating; // 1-5
    private String review;

    private LocalDateTime createdAt;
}