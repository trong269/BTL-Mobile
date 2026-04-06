package com.bookapp.service;

import com.bookapp.model.Review;
import com.bookapp.repository.BookRepository;
import com.bookapp.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;

    public ReviewService(ReviewRepository reviewRepository, BookRepository bookRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
    }

    public List<Review> getByBookId(String bookId) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);
    }

    public Review addReview(String bookId, String userId, int rating, String reviewText) {
        Review review = new Review();
        review.setBookId(bookId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setReview(reviewText);
        review.setCreatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        // Cập nhật avgRating của sách
        List<Review> allReviews = reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);
        OptionalDouble avg = allReviews.stream().mapToInt(Review::getRating).average();
        if (avg.isPresent()) {
            bookRepository.findById(bookId).ifPresent(book -> {
                book.setAvgRating(Math.round(avg.getAsDouble() * 10.0) / 10.0);
                bookRepository.save(book);
            });
        }

        return saved;
    }

    public void deleteReview(String reviewId) {
        reviewRepository.deleteById(reviewId);
    }
}
