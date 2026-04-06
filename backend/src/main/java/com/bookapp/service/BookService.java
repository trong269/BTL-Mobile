package com.bookapp.service;

import com.bookapp.dto.BookCategoryDto;
import com.bookapp.dto.BookResponseDto;
import com.bookapp.model.Book;
import com.bookapp.model.BookCategory;
import com.bookapp.model.Category;
import com.bookapp.repository.BookCategoryRepository;
import com.bookapp.repository.BookRepository;
import com.bookapp.repository.CategoryRepository;
import com.bookapp.repository.ChapterRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final ChapterRepository chapterRepository;
    private static final int TOP_LIMIT = 5;

    public BookService(
            BookRepository bookRepository,
            CategoryRepository categoryRepository,
            BookCategoryRepository bookCategoryRepository,
            ChapterRepository chapterRepository
    ) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.bookCategoryRepository = bookCategoryRepository;
        this.chapterRepository = chapterRepository;
    }

    public List<BookResponseDto> findAll() {
        return toResponseList(bookRepository.findAll());
    }

    public Optional<BookResponseDto> findById(String id) {
        return bookRepository.findById(id).map(this::toResponse);
    }

    public Book getById(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    public List<BookResponseDto> findFeatured() {
        return toResponseList(bookRepository.findByFeaturedTrue());
    }

    public List<BookResponseDto> findNewBooks() {
        return toResponseList(bookRepository.findAllByOrderByCreatedAtDesc());
    }

    public List<BookResponseDto> findByCategory(String categoryId) {
        List<BookCategory> mappings = bookCategoryRepository.findByCategoryId(categoryId);
        List<String> orderedBookIds = mappings.stream()
                .map(BookCategory::getBookId)
                .filter(bookId -> bookId != null)
                .map(ObjectId::toHexString)
                .distinct()
                .toList();

        if (orderedBookIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> orderIndex = buildOrderIndex(orderedBookIds);
        List<Book> books = bookRepository.findAllById(orderedBookIds);
        books.sort(Comparator.comparingInt(book -> orderIndex.getOrDefault(book.getId(), Integer.MAX_VALUE)));

        return toResponseList(books);
    }

    public List<BookResponseDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return toResponseList(bookRepository.searchByKeyword(keyword.trim()));
    }

    public Book create(Book book) {
        normalize(book);
        List<String> resolvedCategoryIds = resolveCategoryIdsForWrite(book);

        book.setId(null);
        book.setCategoryId(null);
        book.setCategories(new ArrayList<>());
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        book.setTotalChapters(0);

        Book saved = bookRepository.save(book);
        syncBookCategories(saved.getId(), resolvedCategoryIds);
        return saved;
    }

    public Book update(String id, Book payload) {
        Book existing = getById(id);
        boolean shouldSyncCategoryMappings = shouldSyncCategoryMappings(payload);
        List<String> resolvedCategoryIds = shouldSyncCategoryMappings
                ? resolveCategoryIdsForWrite(payload)
                : Collections.emptyList();

        existing.setTitle(payload.getTitle());
        existing.setAuthor(payload.getAuthor());
        existing.setDescription(payload.getDescription());
        existing.setCoverImage(payload.getCoverImage());
        existing.setPublisher(payload.getPublisher());
        existing.setPublishDate(payload.getPublishDate());
        existing.setStatus(payload.getStatus());
        existing.setCategoryId(null);
        existing.setCategories(new ArrayList<>());
        existing.setTags(payload.getTags());
        existing.setFeatured(payload.isFeatured());
        existing.setUpdatedAt(LocalDateTime.now());
        normalize(existing);

        Book saved = bookRepository.save(existing);

        if (shouldSyncCategoryMappings) {
            syncBookCategories(saved.getId(), resolvedCategoryIds);
        }

        return saved;
    }

    public void delete(String id) {
        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }

        toObjectId(id).ifPresent(chapterRepository::deleteByBookId);

        toObjectId(id)
                .map(bookCategoryRepository::findByBookId)
                .filter(mappings -> !mappings.isEmpty())
                .ifPresent(bookCategoryRepository::deleteAll);

        bookRepository.deleteById(id);
    }

    public void syncTotalChapters(String bookId) {
        Book book = getById(bookId);
        ObjectId bookObjectId = toObjectId(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid book id"));
        book.setTotalChapters((int) chapterRepository.countByBookId(bookObjectId));
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);
    }

    public void updateAvgRating(String bookId, double newAvg) {
        bookRepository.findById(bookId).ifPresent(book -> {
            book.setAvgRating(newAvg);
            bookRepository.save(book);
        });
    }

    public List<BookResponseDto> findTopWeek() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        List<Book> topWeek = bookRepository.findByCreatedAtGreaterThanEqualOrderByViewsDescAvgRatingDesc(
                fromDate,
                PageRequest.of(0, TOP_LIMIT)
        );

        if (topWeek.isEmpty()) {
            return toResponseList(bookRepository.findAllByOrderByViewsDescAvgRatingDesc(PageRequest.of(0, TOP_LIMIT)));
        }
        return toResponseList(topWeek);
    }

    public List<BookResponseDto> findTopMonth() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);
        List<Book> topMonth = bookRepository.findByCreatedAtGreaterThanEqualOrderByViewsDescAvgRatingDesc(
                fromDate,
                PageRequest.of(0, TOP_LIMIT)
        );

        if (topMonth.isEmpty()) {
            return toResponseList(bookRepository.findAllByOrderByViewsDescAvgRatingDesc(PageRequest.of(0, TOP_LIMIT)));
        }
        return toResponseList(topMonth);
    }

    private List<BookResponseDto> toResponseList(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> bookIds = books.stream()
                .map(Book::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        List<ObjectId> objectBookIds = toObjectIds(bookIds);

        Map<String, List<BookCategory>> mappingByBookId = objectBookIds.isEmpty()
                ? Collections.emptyMap()
                : bookCategoryRepository.findByBookIdIn(objectBookIds).stream()
                .filter(mapping -> mapping.getBookId() != null)
                .collect(Collectors.groupingBy(mapping -> mapping.getBookId().toHexString()));

        Set<String> categoryIds = mappingByBookId.values().stream()
                .flatMap(Collection::stream)
                .map(BookCategory::getCategoryId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Category> categoriesById = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.findByIdIn(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        return books.stream()
                .map(book -> toResponse(book, mappingByBookId.getOrDefault(book.getId(), Collections.emptyList()), categoriesById))
                .toList();
    }

    private BookResponseDto toResponse(Book book) {
        List<BookResponseDto> responses = toResponseList(List.of(book));
        return responses.isEmpty() ? null : responses.get(0);
    }

    private BookResponseDto toResponse(
            Book book,
            List<BookCategory> bookCategories,
            Map<String, Category> categoriesById
    ) {
        List<BookCategory> sortedMappings = sortMappings(bookCategories);

        List<BookCategoryDto> categoryObjects = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();
        Set<String> addedCategoryIds = new LinkedHashSet<>();

        for (BookCategory mapping : sortedMappings) {
            Category category = categoriesById.get(mapping.getCategoryId());
            if (category == null || !addedCategoryIds.add(category.getId())) {
                continue;
            }
            categoryObjects.add(new BookCategoryDto(category.getId(), category.getName(), category.getDescription()));
            categoryNames.add(category.getName());
        }

        String resolvedCategoryId = sortedMappings.stream()
                .map(BookCategory::getCategoryId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse(null);

        return new BookResponseDto(
                book.getId(),
                book.getSourceBookId(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getCoverImage(),
                book.getPublisher(),
                book.getPublishDate(),
                book.getStatus(),
                resolvedCategoryId,
                categoryNames,
                categoryObjects,
                book.getTags() == null ? Collections.emptyList() : book.getTags(),
                book.getTotalChapters(),
                book.getTotalPages(),
                book.getViews(),
                book.getAvgRating(),
                book.isFeatured(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }

    private List<BookCategory> sortMappings(List<BookCategory> bookCategories) {
        if (bookCategories == null || bookCategories.isEmpty()) {
            return Collections.emptyList();
        }
        return bookCategories.stream()
                .sorted(
                        Comparator
                                .comparing((BookCategory mapping) -> !Boolean.TRUE.equals(mapping.getPrimary()))
                                .thenComparing(BookCategory::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                )
                .toList();
    }

    private Map<String, Integer> buildOrderIndex(List<String> orderedIds) {
        Map<String, Integer> orderIndex = new java.util.HashMap<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            orderIndex.put(orderedIds.get(i), i);
        }
        return orderIndex;
    }

    private boolean shouldSyncCategoryMappings(Book payload) {
        if (payload == null) {
            return false;
        }
        if (payload.getCategories() != null) {
            return true;
        }
        return payload.getCategoryId() != null && !payload.getCategoryId().isBlank();
    }

    private List<String> resolveCategoryIdsForWrite(Book payload) {
        if (payload == null) {
            return Collections.emptyList();
        }

        Set<String> resolvedIds = new LinkedHashSet<>();

        resolveCategoryId(payload.getCategoryId()).ifPresent(resolvedIds::add);

        if (payload.getCategories() != null) {
            for (String value : payload.getCategories()) {
                resolveCategoryId(value).ifPresent(resolvedIds::add);
            }
        }

        return new ArrayList<>(resolvedIds);
    }

    private Optional<String> resolveCategoryId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        String value = rawValue.trim();

        return categoryRepository.findById(value)
                .map(Category::getId)
                .or(() -> categoryRepository.findByName(value).map(Category::getId));
    }

    private void syncBookCategories(String bookId, List<String> categoryIds) {
        ObjectId bookObjectId = toObjectId(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid book id"));

        List<BookCategory> existingMappings = bookCategoryRepository.findByBookId(bookObjectId);
        Set<String> targetCategoryIds = new LinkedHashSet<>(categoryIds);

        Map<String, BookCategory> existingByCategoryId = existingMappings.stream()
                .filter(mapping -> mapping.getCategoryId() != null && !mapping.getCategoryId().isBlank())
                .collect(Collectors.toMap(BookCategory::getCategoryId, Function.identity(), (left, right) -> left));

        List<BookCategory> toSave = new ArrayList<>();
        int index = 0;
        for (String categoryId : targetCategoryIds) {
            BookCategory mapping = existingByCategoryId.getOrDefault(categoryId, new BookCategory());
            mapping.setBookId(bookObjectId);
            mapping.setCategoryId(categoryId);
            mapping.setPrimary(index == 0);
            if (mapping.getCreatedAt() == null) {
                mapping.setCreatedAt(LocalDateTime.now());
            }
            toSave.add(mapping);
            index++;
        }

        List<BookCategory> toDelete = existingMappings.stream()
                .filter(mapping -> mapping.getCategoryId() == null || !targetCategoryIds.contains(mapping.getCategoryId()))
                .toList();

        if (!toDelete.isEmpty()) {
            bookCategoryRepository.deleteAll(toDelete);
        }

        if (!toSave.isEmpty()) {
            bookCategoryRepository.saveAll(toSave);
        }
    }

    private List<ObjectId> toObjectIds(List<String> ids) {
        return ids.stream()
                .map(this::toObjectId)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ObjectId> toObjectId(String id) {
        if (id == null || id.isBlank() || !ObjectId.isValid(id)) {
            return Optional.empty();
        }
        return Optional.of(new ObjectId(id));
    }

    private void normalize(Book book) {
        if (book.getCategories() == null) {
            book.setCategories(new ArrayList<>());
        }
        if (book.getTags() == null) {
            book.setTags(new ArrayList<>());
        }
        if (book.getStatus() == null || book.getStatus().isBlank()) {
            book.setStatus("Sẵn sàng");
        }
        if (book.getCoverImage() == null) {
            book.setCoverImage("");
        }
        if (book.getPublisher() == null) {
            book.setPublisher("");
        }
        if (book.getPublishDate() == null) {
            book.setPublishDate("");
        }
        if (book.getDescription() == null) {
            book.setDescription("");
        }
        if (book.getSourceBookId() == null || book.getSourceBookId().isBlank()) {
            book.setSourceBookId("admin-" + System.nanoTime());
        }
    }
}
