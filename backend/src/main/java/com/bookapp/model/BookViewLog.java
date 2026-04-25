package com.bookapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "book_view_logs")
public class BookViewLog {
    @Id
    private String id;
    private String bookId;
    private String userId;
    private LocalDateTime viewedAt;
}
