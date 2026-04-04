package com.bookapp.service;

import com.bookapp.model.Book;
import com.bookapp.repository.BookRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    private static final int TOP_LIMIT = 5;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Optional<Book> findById(String id) {
        return bookRepository.findById(id);
    }

    public List<Book> findFeatured() {
        return bookRepository.findByFeaturedTrue();
    }

    public List<Book> findNewBooks() {
        return bookRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Book> findByCategory(String categoryId) {
        return bookRepository.findByCategoryId(categoryId);
    }

    public List<Book> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookRepository.findAll();
        }
        return bookRepository.searchByKeyword(keyword.trim());
    }

    public Book save(Book book) {
        return bookRepository.save(book);
    }

    public void updateAvgRating(String bookId, double newAvg) {
        bookRepository.findById(bookId).ifPresent(book -> {
            book.setAvgRating(newAvg);
            bookRepository.save(book);
        });
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
