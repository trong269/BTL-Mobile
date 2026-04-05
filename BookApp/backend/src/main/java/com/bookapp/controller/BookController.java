package com.bookapp.controller;

import com.bookapp.dto.BookResponseDto;
import com.bookapp.model.Book;
import com.bookapp.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookResponseDto> findAll() {
        return bookService.findAll();
    }

    @GetMapping("/featured")
    public List<BookResponseDto> findFeatured() {
        return bookService.findFeatured();
    }

    @GetMapping("/new")
    public List<BookResponseDto> findNew() {
        return bookService.findNewBooks();
    }

    @GetMapping("/search")
    public List<BookResponseDto> search(@RequestParam(value = "q", defaultValue = "") String keyword) {
        return bookService.search(keyword);
    }

    @GetMapping("/category/{categoryId}")
    public List<BookResponseDto> findByCategory(@PathVariable String categoryId) {
        return bookService.findByCategory(categoryId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> findById(@PathVariable String id) {
        return bookService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Book create(@RequestBody Book book) {
        return bookService.create(book);
    }

    @PutMapping("/{id}")
    public Book update(@PathVariable String id, @RequestBody Book book) {
        return bookService.update(id, book);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/top-week")
    public List<BookResponseDto> findTopWeekBooks() {
        return bookService.findTopWeek();
    }

    @GetMapping("/top-month")
    public List<BookResponseDto> findTopMonthBooks() {
        return bookService.findTopMonth();
    }
}
