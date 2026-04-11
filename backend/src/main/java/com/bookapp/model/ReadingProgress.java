package com.bookapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reading_progress")
public class ReadingProgress {

    @Id
    private String id;

    private String userId;
    private String bookId;
    private String chapterId;

    private int chapterProgressPercent; 

    private LocalDateTime updatedAt;
}