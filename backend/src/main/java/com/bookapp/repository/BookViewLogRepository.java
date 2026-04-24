package com.bookapp.repository;

import com.bookapp.dto.TopBookIdDto;
import com.bookapp.model.BookViewLog;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface BookViewLogRepository extends MongoRepository<BookViewLog, String> {
    List<BookViewLog> findByViewedAtAfter(LocalDateTime dateTime);

    @Aggregation(pipeline = {
        "{ $match: { viewedAt: { $gte: ?0 } } }",
        "{ $group: { _id: '$bookId', count: { $sum: 1 } } }",
        "{ $sort: { count: -1 } }",
        "{ $limit: ?1 }",
        "{ $project: { _id: 0, id: '$_id' } }"
    })
    List<TopBookIdDto> findTopBookIdsByViewCount(LocalDateTime since, int limit);
}
