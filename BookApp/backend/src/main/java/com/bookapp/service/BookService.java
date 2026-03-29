package com.bookapp.service;

import com.bookapp.model.Book;
import com.bookapp.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;

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
}
