package com.bookapp.service;

import com.bookapp.model.Favorite;
import com.bookapp.repository.FavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public List<Favorite> getByUserId(String userId) {
        return favoriteRepository.findByUserId(userId);
    }

    public boolean isFavorite(String userId, String bookId) {
        return favoriteRepository.existsByUserIdAndBookId(userId, bookId);
    }

    @Transactional
    public Map<String, Object> toggleFavorite(String userId, String bookId) {
        Map<String, Object> result = new HashMap<>();
        if (favoriteRepository.existsByUserIdAndBookId(userId, bookId)) {
            favoriteRepository.deleteByUserIdAndBookId(userId, bookId);
            result.put("favorited", false);
            result.put("message", "Da xoa khoi yeu thich");
        } else {
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setBookId(bookId);
            fav.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(fav);
            result.put("favorited", true);
            result.put("message", "Da them vao yeu thich");
        }
        return result;
    }
}
