package com.bookapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChapterRequestDto {
    private int chapterNumber;
    private String title;
    private String content;
}
