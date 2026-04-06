package com.bookapp.service;

import com.bookapp.model.Category;
import com.bookapp.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findById(String id) {
        return categoryRepository.findById(id);
    }

    public Category getById(String id) {
        return findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    public Category create(Category category) {
        normalize(category);
        if (categoryRepository.existsByNameIgnoreCase(category.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        category.setId(null);
        category.setSlug(toSlug(category.getName()));
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        return categoryRepository.save(category);
    }

    public Category update(String id, Category payload) {
        Category existing = getById(id);
        existing.setName(payload.getName());
        existing.setDescription(payload.getDescription());
        normalize(existing);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(existing.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }
        existing.setSlug(toSlug(existing.getName()));
        existing.setUpdatedAt(LocalDateTime.now());
        return categoryRepository.save(existing);
    }

    public void delete(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        categoryRepository.deleteById(id);
    }

    private void normalize(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is required");
        }
        category.setName(category.getName().trim());
        if (category.getDescription() == null) {
            category.setDescription("");
        } else {
            category.setDescription(category.getDescription().trim());
        }
    }

    private String toSlug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category slug is invalid");
        }

        return normalized;
    }
}
