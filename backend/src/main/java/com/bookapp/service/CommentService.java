package com.bookapp.service;

import com.bookapp.dto.CommentResponseDto;
import com.bookapp.model.Comment;
import com.bookapp.model.User;
import com.bookapp.repository.CommentRepository;
import com.bookapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public List<CommentResponseDto> getByBookId(String bookId) {
        List<Comment> comments = commentRepository.findByBookIdOrderByCreatedAtDesc(bookId);
        return comments.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public CommentResponseDto addComment(String bookId, String userId, String content) {
        Comment comment = new Comment();
        comment.setBookId(bookId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);
        return convertToDto(saved);
    }

    private CommentResponseDto convertToDto(Comment comment) {
        CommentResponseDto dto = new CommentResponseDto();
        dto.setId(comment.getId());
        dto.setUserId(comment.getUserId());
        dto.setBookId(comment.getBookId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());

        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            dto.setUsername(user.getUsername());
            dto.setFullName(user.getFullName());
            dto.setAvatar(user.getAvatar());
        });

        return dto;
    }

    public void deleteComment(String commentId) {
        commentRepository.deleteById(commentId);
    }
}
