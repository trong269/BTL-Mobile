package com.bookapp.repository;

import com.bookapp.model.BookCategory;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface BookCategoryRepository extends MongoRepository<BookCategory, String> {
    List<BookCategory> findByBookIdIn(Collection<ObjectId> bookIds);

    List<BookCategory> findByCategoryId(String categoryId);

    List<BookCategory> findByBookId(ObjectId bookId);
}
