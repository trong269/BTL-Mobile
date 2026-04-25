package com.bookapp.repository;

import com.bookapp.model.ReadingProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReadingProgressRepository extends MongoRepository<ReadingProgress, String> {
    Optional<ReadingProgress> findByUserIdAndBookId(String userId, String bookId);
}
