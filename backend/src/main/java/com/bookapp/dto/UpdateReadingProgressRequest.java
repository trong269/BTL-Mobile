package com.bookapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateReadingProgressRequest {
    private String userId;
    private String bookId;
    private String chapterId;
    private Integer chapterProgressPercent;
}
