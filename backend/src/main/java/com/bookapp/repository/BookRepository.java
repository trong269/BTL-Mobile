package com.bookapp.repository;

import com.bookapp.model.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {

	boolean existsByTitleIgnoreCase(String title);

	Optional<Book> findFirstByTitleIgnoreCase(String title);

	List<Book> findByCreatedAtGreaterThanEqualOrderByViewsDescAvgRatingDesc(LocalDateTime createdAt, Pageable pageable);

	List<Book> findAllByOrderByViewsDescAvgRatingDesc(Pageable pageable);

    List<Book> findByCategoryId(String categoryId);

    List<Book> findByFeaturedTrue();

    List<Book> findAllByOrderByCreatedAtDesc();

    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByAuthorContainingIgnoreCase(String author);

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'author': { $regex: ?0, $options: 'i' } }, { 'tags': { $regex: ?0, $options: 'i' } } ] }")
    List<Book> searchByKeyword(String keyword);
}
