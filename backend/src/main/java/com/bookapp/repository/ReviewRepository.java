package com.bookapp.repository;

import com.bookapp.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByBookIdOrderByCreatedAtDesc(String bookId);
    List<Review> findByUserId(String userId);
    boolean existsByUserIdAndBookId(String userId, String bookId);
    Optional<Review> findByUserIdAndBookId(String userId, String bookId);
}
