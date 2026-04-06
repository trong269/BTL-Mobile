package com.bookapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "book_categories")
public class BookCategory {

    @Id
    private String id;

    private ObjectId bookId;
    private String categoryId;

    @Field("isPrimary")
    private Boolean primary;

    private String confidence;
    private LocalDateTime createdAt;
}
