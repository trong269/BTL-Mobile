package com.bookapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookResponseDto {

    private String id;
    private String sourceBookId;

    private String title;
    private String author;
    private String description;
    private String coverImage;
    private String publisher;
    private String publishDate;
    private String status;

    private String categoryId;
    private List<String> categories;
    private List<BookCategoryDto> categoryObjects;
    private List<String> tags;

    private int totalChapters;
    private int totalPages;
    private int views;
    private double avgRating;

    private boolean featured;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
