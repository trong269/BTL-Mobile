package com.bookapp.service;

import com.bookapp.model.Book;
import com.bookapp.repository.BookRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookService {
    private BookRepository bookRepository;
    private static final int TOP_LIMIT = 5;

    public  BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public List<Book> findTopWeek() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        List<Book> topWeek = bookRepository.findByCreatedAtGreaterThanEqualOrderByViewsDescAvgRatingDesc(
            fromDate,
            PageRequest.of(0, TOP_LIMIT)
        );

        if (topWeek.isEmpty()) {
            return bookRepository.findAllByOrderByViewsDescAvgRatingDesc(PageRequest.of(0, TOP_LIMIT));
        }
        return topWeek;
    }

    public List<Book> findTopMonth() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);
        List<Book> topMonth = bookRepository.findByCreatedAtGreaterThanEqualOrderByViewsDescAvgRatingDesc(
            fromDate,
            PageRequest.of(0, TOP_LIMIT)
        );

        if (topMonth.isEmpty()) {
            return bookRepository.findAllByOrderByViewsDescAvgRatingDesc(PageRequest.of(0, TOP_LIMIT));
        }
        return topMonth;
    }
}
