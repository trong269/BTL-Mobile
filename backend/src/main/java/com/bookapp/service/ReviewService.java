package com.bookapp.service;

import com.bookapp.dto.ReviewResponseDto;
import com.bookapp.model.Review;
import com.bookapp.model.User;
import com.bookapp.repository.BookRepository;
import com.bookapp.repository.ReviewRepository;
import com.bookapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, BookRepository bookRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public List<ReviewResponseDto> getByBookId(String bookId) {
        List<Review> reviews = reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);
        return reviews.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public ReviewResponseDto addReview(String bookId, String userId, int rating, String reviewText) {
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

        return convertToDto(saved);
    }

    private ReviewResponseDto convertToDto(Review review) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(review.getId());
        dto.setUserId(review.getUserId());
        dto.setBookId(review.getBookId());
        dto.setRating(review.getRating());
        dto.setReview(review.getReview());
        dto.setCreatedAt(review.getCreatedAt());

        userRepository.findById(review.getUserId()).ifPresent(user -> {
            dto.setUsername(user.getUsername());
            dto.setFullName(user.getFullName());
            dto.setAvatar(user.getAvatar());
        });

        return dto;
    }

    public void deleteReview(String reviewId) {
        reviewRepository.deleteById(reviewId);
    }
}
