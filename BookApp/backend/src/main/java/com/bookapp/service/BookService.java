package com.bookapp.service;

import com.bookapp.model.Book;
import com.bookapp.repository.BookRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public Book getById(String id) {
        return findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
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

    public Book create(Book book) {
        normalize(book);
        book.setId(null);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }

    public Book update(String id, Book payload) {
        Book existing = getById(id);
        existing.setTitle(payload.getTitle());
        existing.setAuthor(payload.getAuthor());
        existing.setDescription(payload.getDescription());
        existing.setCoverImage(payload.getCoverImage());
        existing.setPublisher(payload.getPublisher());
        existing.setPublishDate(payload.getPublishDate());
        existing.setStatus(payload.getStatus());
        existing.setCategoryId(payload.getCategoryId());
        existing.setCategories(payload.getCategories());
        existing.setTags(payload.getTags());
        existing.setFeatured(payload.isFeatured());
        existing.setUpdatedAt(LocalDateTime.now());
        normalize(existing);
        return bookRepository.save(existing);
    }

    public void delete(String id) {
        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }
        bookRepository.deleteById(id);
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

    private void normalize(Book book) {
        if (book.getCategories() == null) {
            book.setCategories(new ArrayList<>());
        }
        if (book.getTags() == null) {
            book.setTags(new ArrayList<>());
        }
        if (book.getStatus() == null || book.getStatus().isBlank()) {
            book.setStatus("Sẵn sàng");
        }
        if (book.getCoverImage() == null) {
            book.setCoverImage("");
        }
        if (book.getPublisher() == null) {
            book.setPublisher("");
        }
        if (book.getPublishDate() == null) {
            book.setPublishDate("");
        }
        if (book.getDescription() == null) {
            book.setDescription("");
        }
        if (book.getSourceBookId() == null || book.getSourceBookId().isBlank()) {
            book.setSourceBookId("admin-" + System.nanoTime());
        }
    }
}
