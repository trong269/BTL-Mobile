package com.bookapp.controller;

import com.bookapp.service.FavoriteService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    // POST /api/favorites/toggle
    // Body: { "userId": "...", "bookId": "..." }
    @PostMapping("/toggle")
    public Map<String, Object> toggle(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String bookId = body.get("bookId");
        return favoriteService.toggleFavorite(userId, bookId);
    }

    // GET /api/favorites/check?userId=...&bookId=...
    @GetMapping("/check")
    public Map<String, Object> check(
            @RequestParam String userId,
            @RequestParam String bookId) {
        boolean fav = favoriteService.isFavorite(userId, bookId);
        return Map.of("favorited", fav);
    }

    // GET /api/favorites/user/{userId}
    @GetMapping("/user/{userId}")
    public Object getByUser(@PathVariable String userId) {
        return favoriteService.getByUserId(userId);
    }
}
