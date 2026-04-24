package com.bookapp.controller;

import com.bookapp.dto.CommentResponseDto;
import com.bookapp.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // GET /api/comments/book/{bookId}
    @GetMapping("/book/{bookId}")
    public List<CommentResponseDto> getByBook(@PathVariable String bookId) {
        return commentService.getByBookId(bookId);
    }

    // POST /api/comments
    // Body: { "bookId": "...", "userId": "...", "content": "..." }
    @PostMapping
    public CommentResponseDto addComment(@RequestBody Map<String, String> body) {
        String bookId = body.get("bookId");
        String userId = body.get("userId");
        String content = body.get("content");
        return commentService.addComment(bookId, userId, content);
    }

    // DELETE /api/comments/{id}
    @DeleteMapping("/{id}")
    public Map<String, String> deleteComment(@PathVariable String id) {
        commentService.deleteComment(id);
        return Map.of("message", "Da xoa binh luan");
    }
}
