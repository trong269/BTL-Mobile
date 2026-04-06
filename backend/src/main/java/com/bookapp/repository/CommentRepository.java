package com.bookapp.repository;

import com.bookapp.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByBookIdOrderByCreatedAtDesc(String bookId);
    List<Comment> findByUserId(String userId);
}
