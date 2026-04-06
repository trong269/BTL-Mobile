package com.bookapp.service;

import com.bookapp.dto.ChapterRequestDto;
import com.bookapp.dto.ChapterResponseDto;
import com.bookapp.model.Chapter;
import com.bookapp.repository.ChapterRepository;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookService bookService;

    public ChapterService(ChapterRepository chapterRepository, BookService bookService) {
        this.chapterRepository = chapterRepository;
        this.bookService = bookService;
    }

    public List<ChapterResponseDto> findByBookId(String bookId) {
        bookService.getById(bookId);
        ObjectId bookObjectId = toObjectId(bookId);
        return chapterRepository.findByBookIdOrderByChapterNumberAsc(bookObjectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ChapterResponseDto findByBookIdAndChapterId(String bookId, String chapterId) {
        bookService.getById(bookId);
        return toResponse(getChapter(bookId, chapterId));
    }

    public ChapterResponseDto create(String bookId, ChapterRequestDto payload) {
        bookService.getById(bookId);
        validate(payload, null, bookId);

        ObjectId bookObjectId = toObjectId(bookId);

        Chapter chapter = new Chapter();
        chapter.setBookId(bookObjectId);
        chapter.setChapterNumber(payload.getChapterNumber());
        chapter.setTitle(payload.getTitle().trim());
        chapter.setContent(normalizeContent(payload.getContent()));

        Chapter saved = chapterRepository.save(chapter);
        bookService.syncTotalChapters(bookId);
        return toResponse(saved);
    }

    public ChapterResponseDto update(String bookId, String chapterId, ChapterRequestDto payload) {
        bookService.getById(bookId);
        Chapter existing = getChapter(bookId, chapterId);
        validate(payload, chapterId, bookId);

        existing.setChapterNumber(payload.getChapterNumber());
        existing.setTitle(payload.getTitle().trim());
        existing.setContent(normalizeContent(payload.getContent()));

        Chapter saved = chapterRepository.save(existing);
        bookService.syncTotalChapters(bookId);
        return toResponse(saved);
    }

    public void delete(String bookId, String chapterId) {
        bookService.getById(bookId);
        Chapter existing = getChapter(bookId, chapterId);
        chapterRepository.delete(existing);
        bookService.syncTotalChapters(bookId);
    }

    private Chapter getChapter(String bookId, String chapterId) {
        return chapterRepository.findByIdAndBookId(chapterId, toObjectId(bookId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
    }

    private void validate(ChapterRequestDto payload, String chapterId, String bookId) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chapter payload is required");
        }
        if (payload.getChapterNumber() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chapter number must be greater than 0");
        }
        if (payload.getTitle() == null || payload.getTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chapter title is required");
        }

        ObjectId bookObjectId = toObjectId(bookId);

        boolean duplicate = chapterId == null
                ? chapterRepository.existsByBookIdAndChapterNumber(bookObjectId, payload.getChapterNumber())
                : chapterRepository.existsByBookIdAndChapterNumberAndIdNot(bookObjectId, payload.getChapterNumber(), chapterId);

        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chapter number already exists");
        }
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content;
    }

    private ChapterResponseDto toResponse(Chapter chapter) {
        return new ChapterResponseDto(
                chapter.getId(),
                chapter.getBookId().toHexString(),
                chapter.getChapterNumber(),
                chapter.getTitle(),
                chapter.getContent()
        );
    }

    private ObjectId toObjectId(String id) {
        if (!ObjectId.isValid(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid book id");
        }
        return new ObjectId(id);
    }
}
