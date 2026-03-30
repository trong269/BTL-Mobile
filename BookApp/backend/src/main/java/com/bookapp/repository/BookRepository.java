package com.bookapp.repository;

import com.bookapp.model.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {

	boolean existsByTitleIgnoreCase(String title);

	Optional<Book> findFirstByTitleIgnoreCase(String title);

	List<Book> findByCreatedAtGreaterThanEqualOrderByViewsDescAvgRatingDesc(LocalDateTime createdAt, Pageable pageable);

	List<Book> findAllByOrderByViewsDescAvgRatingDesc(Pageable pageable);

}
