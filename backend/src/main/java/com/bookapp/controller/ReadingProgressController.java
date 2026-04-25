package com.bookapp.controller;

import com.bookapp.dto.EnsureReadingProgressRequest;
import com.bookapp.dto.ReadingProgressResponseDto;
import com.bookapp.dto.UpdateReadingProgressRequest;
import com.bookapp.service.ReadingProgressService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reading-progress")
public class ReadingProgressController {

    private final ReadingProgressService readingProgressService;

    public ReadingProgressController(ReadingProgressService readingProgressService) {
        this.readingProgressService = readingProgressService;
    }

    @PostMapping("/ensure")
    public ReadingProgressResponseDto ensure(@RequestBody EnsureReadingProgressRequest payload) {
        return readingProgressService.ensure(payload);
    }

    @PutMapping
    public ReadingProgressResponseDto update(@RequestBody UpdateReadingProgressRequest payload) {
        return readingProgressService.update(payload);
    }
}
