package com.bookapp.service;

import com.bookapp.dto.EnsureReadingProgressRequest;
import com.bookapp.dto.ReadingProgressResponseDto;
import com.bookapp.dto.UpdateReadingProgressRequest;
import com.bookapp.model.Chapter;
import com.bookapp.model.ReadingProgress;
import com.bookapp.repository.ChapterRepository;
import com.bookapp.repository.ReadingProgressRepository;
import com.bookapp.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class ReadingProgressService {

    private final ReadingProgressRepository readingProgressRepository;
    private final ChapterRepository chapterRepository;
    private final BookService bookService;
    private final UserRepository userRepository;

    public ReadingProgressService(
            ReadingProgressRepository readingProgressRepository,
            ChapterRepository chapterRepository,
            BookService bookService,
            UserRepository userRepository
    ) {
        this.readingProgressRepository = readingProgressRepository;
        this.chapterRepository = chapterRepository;
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    public ReadingProgressResponseDto ensure(EnsureReadingProgressRequest payload) {
        validateIdentity(payload.getUserId(), payload.getBookId());
        bookService.getById(payload.getBookId());
        bookService.incrementViews(payload.getBookId().trim());

        ReadingProgress existing = readingProgressRepository
                .findByUserIdAndBookId(payload.getUserId(), payload.getBookId())
                .orElse(null);

        updateUserActivity(payload.getUserId());

        if (existing != null) {
            return toResponse(existing);
        }

        ObjectId bookObjectId = toObjectId(payload.getBookId());
        Chapter firstChapter = chapterRepository.findByBookIdOrderByChapterNumberAsc(bookObjectId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Book has no chapters"
                ));

        ReadingProgress created = new ReadingProgress();
        created.setUserId(payload.getUserId().trim());
        created.setBookId(payload.getBookId().trim());
        created.setChapterId(firstChapter.getId());
        created.setChapterProgressPercent(0);
        created.setUpdatedAt(LocalDateTime.now());

        return toResponse(readingProgressRepository.save(created));
    }

    public ReadingProgressResponseDto update(UpdateReadingProgressRequest payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reading progress payload is required");
        }
        validateIdentity(payload.getUserId(), payload.getBookId());

        String chapterId = payload.getChapterId();
        if (chapterId == null || chapterId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chapterId is required");
        }

        ObjectId bookObjectId = toObjectId(payload.getBookId());
        chapterRepository.findByIdAndBookId(chapterId, bookObjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chapter does not belong to book"));

        int progress = payload.getChapterProgressPercent() == null ? 0 : payload.getChapterProgressPercent();
        progress = Math.max(0, Math.min(100, progress));

        ReadingProgress existing = readingProgressRepository
                .findByUserIdAndBookId(payload.getUserId().trim(), payload.getBookId().trim())
                .orElseGet(() -> {
                    ReadingProgress created = new ReadingProgress();
                    created.setUserId(payload.getUserId().trim());
                    created.setBookId(payload.getBookId().trim());
                    return created;
                });

        existing.setChapterId(chapterId);
        existing.setChapterProgressPercent(progress);
        existing.setUpdatedAt(LocalDateTime.now());

        updateUserActivity(payload.getUserId());

        return toResponse(readingProgressRepository.save(existing));
    }

    private void updateUserActivity(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    private void validateIdentity(String userId, String bookId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (bookId == null || bookId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookId is required");
        }
    }

    private ObjectId toObjectId(String id) {
        if (!ObjectId.isValid(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid book id");
        }
        return new ObjectId(id);
    }

    private ReadingProgressResponseDto toResponse(ReadingProgress entity) {
        return new ReadingProgressResponseDto(
                entity.getId(),
                entity.getUserId(),
                entity.getBookId(),
                entity.getChapterId(),
                entity.getChapterProgressPercent(),
                entity.getUpdatedAt()
        );
    }
}
