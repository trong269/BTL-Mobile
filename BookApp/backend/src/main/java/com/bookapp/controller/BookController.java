package com.bookapp.controller;

import com.bookapp.service.BookService;
import com.bookapp.model.Book;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> findAllBooks() {
        return bookService.findAll();
    }

    @GetMapping("/top-week")
    public List<Book> findTopWeekBooks() {
        return bookService.findTopWeek();
    }

    @GetMapping("/top-month")
    public List<Book> findTopMonthBooks() {
        return bookService.findTopMonth();
    }

}
