package com.bookapp.controller;

import com.bookapp.model.Review;
import com.bookapp.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // GET /api/reviews/book/{bookId}
    @GetMapping("/book/{bookId}")
    public List<Review> getByBook(@PathVariable String bookId) {
        return reviewService.getByBookId(bookId);
    }

    // POST /api/reviews
    // Body: { "bookId": "...", "userId": "...", "rating": 5, "review": "..." }
    @PostMapping
    public Review addReview(@RequestBody Map<String, Object> body) {
        String bookId = (String) body.get("bookId");
        String userId = (String) body.get("userId");
        int rating = Integer.parseInt(body.get("rating").toString());
        String reviewText = (String) body.getOrDefault("review", "");
        return reviewService.addReview(bookId, userId, rating, reviewText);
    }

    // DELETE /api/reviews/{id}
    @DeleteMapping("/{id}")
    public Map<String, String> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id);
        return Map.of("message", "Da xoa danh gia");
    }
}
