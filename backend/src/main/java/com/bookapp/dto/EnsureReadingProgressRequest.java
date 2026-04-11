package com.bookapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnsureReadingProgressRequest {
    private String userId;
    private String bookId;
}
