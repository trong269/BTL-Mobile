package com.bookapp.repository;

import com.bookapp.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    List<Category> findByIdIn(Collection<String> ids);

    List<Category> findByNameContainingIgnoreCase(String name);
}
