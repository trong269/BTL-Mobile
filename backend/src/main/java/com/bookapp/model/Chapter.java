package com.bookapp.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chapters")
public class Chapter {

    @Id
    private String id;

    private ObjectId bookId;

    private int chapterNumber;
    private String title;
    private String content;
}