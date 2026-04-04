package com.bookapp.controller;

import com.bookapp.model.Category;
import com.bookapp.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // GET /api/categories
    @GetMapping
    public List<Category> findAll() {
        return categoryService.findAll();
    }

    // GET /api/categories/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Category> findById(@PathVariable String id) {
        return categoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/categories  (dùng cho admin seed data)
    @PostMapping
    public Category create(@RequestBody Category category) {
        return categoryService.save(category);
    }
}
