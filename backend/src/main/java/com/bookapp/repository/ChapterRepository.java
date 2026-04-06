package com.bookapp.repository;

import com.bookapp.model.Chapter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends MongoRepository<Chapter, String> {
    List<Chapter> findByBookIdOrderByChapterNumberAsc(ObjectId bookId);

    Optional<Chapter> findByIdAndBookId(String id, ObjectId bookId);

    boolean existsByBookIdAndChapterNumber(ObjectId bookId, int chapterNumber);

    boolean existsByBookIdAndChapterNumberAndIdNot(ObjectId bookId, int chapterNumber, String id);

    long countByBookId(ObjectId bookId);

    void deleteByBookId(ObjectId bookId);
}
