package com.bookapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "books")
public class Book {

    @Id
    private String id;

    private String title;
    private String author;
    private String description;
    private String summary;

    private String coverImage;
    private String categoryId;

    private int totalChapters;
    private int totalPages;

    private int views;
    private double avgRating;

    private LocalDateTime createdAt;
}