package com.bookapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "favorites")
public class Favorite {

    @Id
    private String id;

    private String userId;
    private String bookId;

    private LocalDateTime createdAt;
}