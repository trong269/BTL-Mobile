package com.bookapp.controller;

import com.bookapp.dto.ChapterRequestDto;
import com.bookapp.dto.ChapterResponseDto;
import com.bookapp.service.ChapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books/{bookId}/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping
    public List<ChapterResponseDto> findByBookId(@PathVariable String bookId) {
        return chapterService.findByBookId(bookId);
    }

    @GetMapping("/{chapterId}")
    public ChapterResponseDto findByBookIdAndChapterId(@PathVariable String bookId, @PathVariable String chapterId) {
        return chapterService.findByBookIdAndChapterId(bookId, chapterId);
    }

    @PostMapping
    public ChapterResponseDto create(@PathVariable String bookId, @RequestBody ChapterRequestDto payload) {
        return chapterService.create(bookId, payload);
    }

    @PutMapping("/{chapterId}")
    public ChapterResponseDto update(
            @PathVariable String bookId,
            @PathVariable String chapterId,
            @RequestBody ChapterRequestDto payload
    ) {
        return chapterService.update(bookId, chapterId, payload);
    }

    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> delete(@PathVariable String bookId, @PathVariable String chapterId) {
        chapterService.delete(bookId, chapterId);
        return ResponseEntity.noContent().build();
    }
}
