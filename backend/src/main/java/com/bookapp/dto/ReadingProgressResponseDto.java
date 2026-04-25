package com.bookapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadingProgressResponseDto {
    private String id;
    private String userId;
    private String bookId;
    private String chapterId;
    private int chapterProgressPercent;
    private LocalDateTime updatedAt;
}
