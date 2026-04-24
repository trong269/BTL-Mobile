package com.bookapp.service;

import com.bookapp.model.Comment;
import com.bookapp.model.User;
import com.bookapp.repository.CommentRepository;
import com.bookapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public List<Comment> getByBookId(String bookId) {
        return commentRepository.findByBookIdOrderByCreatedAtDesc(bookId);
    }

    public Comment addComment(String bookId, String userId, String content) {
        Comment comment = new Comment();
        comment.setBookId(bookId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());

        // Set userName từ User object
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            comment.setUserName(user.getUsername() != null ? user.getUsername() : user.getFullName());
        }

        return commentRepository.save(comment);
    }

    public void deleteComment(String commentId) {
        commentRepository.deleteById(commentId);
    }
}
