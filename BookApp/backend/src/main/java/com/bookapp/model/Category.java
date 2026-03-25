package com.bookapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "categories")
public class Category {

    @Id
    private String id;

    private String name;
    private String description;
}