package com.bookapp.controller;

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

    // GET /api/books
    @GetMapping
    public List<Book> findAll() {
        return bookService.findAll();
    }

    // GET /api/books/featured
    @GetMapping("/featured")
    public List<Book> findFeatured() {
        return bookService.findFeatured();
    }

    // GET /api/books/new
    @GetMapping("/new")
    public List<Book> findNew() {
        return bookService.findNewBooks();
    }

    // GET /api/books/search?q=keyword
    @GetMapping("/search")
    public List<Book> search(@RequestParam(value = "q", defaultValue = "") String keyword) {
        return bookService.search(keyword);
    }

    // GET /api/books/category/{categoryId}
    @GetMapping("/category/{categoryId}")
    public List<Book> findByCategory(@PathVariable String categoryId) {
        return bookService.findByCategory(categoryId);
    }

    // GET /api/books/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Book> findById(@PathVariable String id) {
        return bookService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/books  (dùng cho admin)
    @PostMapping
    public Book create(@RequestBody Book book) {
        return bookService.save(book);
    }
}
