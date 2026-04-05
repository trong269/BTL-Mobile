package com.bookapp.config;

import com.bookapp.model.Book;
import com.bookapp.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BookDataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;

    public BookDataSeeder(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) {
        List<Book> seeds = List.of(
            createBook(
                "Nha Gia Kim",
                "Paulo Coelho",
                "Hanh trinh di tim kho bau va y nghia cua giac mo.",
                "Tieu thuyet truyen cam hung ve su can dam theo duoi dinh menh.",
                "https://picsum.photos/seed/book-alchemist/300/450",
                "novel",
                18,
                220,
                20000,
                4.7,
                15
            ),
            createBook(
                "Dac Nhan Tam",
                "Dale Carnegie",
                "Nghe thuat giao tiep va ung xu giup cai thien moi quan he.",
                "Cuon sach kinh dien ve ky nang song va lanh dao.",
                "https://picsum.photos/seed/book-dacnhantam/300/450",
                "self-help",
                24,
                320,
                22000,
                4.8,
                12
            ),
            createBook(
                "Tu Duy Nhanh Va Cham",
                "Daniel Kahneman",
                "Kham pha co che ra quyet dinh cua bo nao con nguoi.",
                "Sach tam ly hoc ve hai he thong tu duy va nhung dinh kien nhan thuc.",
                "https://picsum.photos/seed/book-thinking/300/450",
                "psychology",
                28,
                410,
                7300,
                4.6,
                75
            ),
            createBook(
                "Nha Dau Tu Thong Minh",
                "Benjamin Graham",
                "Nguyen tac dau tu gia tri va quan tri rui ro ben vung.",
                "Tai lieu quan trong cho nguoi bat dau va nha dau tu dai han.",
                "https://picsum.photos/seed/book-investor/300/450",
                "finance",
                30,
                500,
                6500,
                4.5,
                63
            ),
            createBook(
                "Muon Kiep Nhan Sinh",
                "Nguyen Phong",
                "Nhung cau chuyen ve nhan qua va hanh trinh tinh than.",
                "Tac pham duoc doc gia tre yeu thich trong nhieu nam gan day.",
                "https://picsum.photos/seed/book-muonkiep/300/450",
                "spiritual",
                16,
                260,
                9800,
                4.7,
                40
            ),
            createBook(
                "Cay Cam Ngot Cua Toi",
                "Jose Mauro de Vasconcelos",
                "Cau chuyen cam dong ve tuoi tho va su truong thanh.",
                "Mot tac pham nhe nhang nhung day sau sac.",
                "https://picsum.photos/seed/book-caycamngot/300/450",
                "literature",
                14,
                210,
                10900,
                4.6,
                6
            ),
            createBook(
                "De Men Phieu Luu Ky",
                "To Hoai",
                "Phieu luu cua De Men cung nhung bai hoc ve tinh ban.",
                "Tac pham thieu nhi noi tieng cua van hoc Viet Nam.",
                "https://picsum.photos/seed/book-demen/300/450",
                "children",
                12,
                180,
                6700,
                4.5,
                110
            ),
            createBook(
                "Tuoi Tre Dang Gia Bao Nhieu",
                "Rosie Nguyen",
                "Goc nhin thuc te ve hoc tap, lam viec va trai nghiem.",
                "Sach truyen dong luc cho nguoi tre trong giai doan dinh huong.",
                "https://picsum.photos/seed/book-tuoitre/300/450",
                "inspiration",
                15,
                240,
                11800,
                4.4,
                5
            ),
            createBook(
                "Khong Gia Dinh",
                "Hector Malot",
                "Hanh trinh cua Remi di tim gia dinh va niem tin.",
                "Tieu thuyet kinh dien ve tinh nguoi va y chi song.",
                "https://picsum.photos/seed/book-khonggiadinh/300/450",
                "classic",
                22,
                390,
                6000,
                4.5,
                140
            ),
            createBook(
                "Sherlock Holmes: Dac Vu Cua Bo Oc",
                "Arthur Conan Doyle",
                "Nhung vu an ly ky cua tham tu Sherlock Holmes.",
                "Tap hop truyen trinh tham day hap dan.",
                "https://picsum.photos/seed/book-sherlock/300/450",
                "mystery",
                20,
                330,
                7200,
                4.6,
                66
            ),
            createBook(
                "Lap Trinh Java Can Ban",
                "Nguyen Van A",
                "Tu hoc Java tu can ban den huong doi tuong.",
                "Phu hop cho sinh vien va nguoi moi bat dau lap trinh.",
                "https://picsum.photos/seed/book-java/300/450",
                "technology",
                26,
                360,
                12500,
                4.3,
                3
            ),
            createBook(
                "Clean Code",
                "Robert C. Martin",
                "Nguyen tac viet ma nguon sach va de bao tri.",
                "Sach kinh dien cho lap trinh vien mong muon nang cap tu duy.",
                "https://picsum.photos/seed/book-cleancode/300/450",
                "technology",
                25,
                464,
                7700,
                4.8,
                98
            ),
            createBook(
                "Atomic Habits",
                "James Clear",
                "Xay dung thoi quen tot bang nhung thay doi nho moi ngay.",
                "Huong dan thuc te de cai thien ban than ben vung.",
                "https://picsum.photos/seed/book-atomic/300/450",
                "self-help",
                19,
                320,
                18500,
                4.8,
                18
            ),
            createBook(
                "Nguoi Giau Co Nhat Thanh Babylon",
                "George S. Clason",
                "Bai hoc tai chinh ca nhan duoc ke qua nhung cau chuyen co.",
                "Tac pham de doc va de ap dung cho nguoi muon quan ly tien bac.",
                "https://picsum.photos/seed/book-babylon/300/450",
                "finance",
                13,
                190,
                7100,
                4.7,
                80
            )
        );

        List<Book> booksToSave = new ArrayList<>();
        for (Book book : seeds) {
            Book mergedBook = bookRepository.findFirstByTitleIgnoreCase(book.getTitle())
                .map(existing -> mergeBook(existing, book))
                .orElse(book);
            booksToSave.add(mergedBook);
        }

        List<Book> existingBooks = bookRepository.findAll();
        for (Book existing : existingBooks) {
            boolean needsBackfill = existing.getViews() <= 0
                || existing.getAvgRating() <= 0
                || existing.getCreatedAt() == null;
            if (needsBackfill) {
                existing.setViews(existing.getViews() > 0 ? existing.getViews() : 1200);
                existing.setAvgRating(existing.getAvgRating() > 0 ? existing.getAvgRating() : 4.2);
                existing.setCreatedAt(existing.getCreatedAt() != null ? existing.getCreatedAt() : LocalDateTime.now().minusDays(20));
                booksToSave.add(existing);
            }
        }

        if (!booksToSave.isEmpty()) {
            bookRepository.saveAll(booksToSave);
        }
    }

    private Book mergeBook(Book existing, Book seed) {
        if (isBlank(existing.getAuthor())) {
            existing.setAuthor(seed.getAuthor());
        }
        if (isBlank(existing.getDescription())) {
            existing.setDescription(seed.getDescription());
        }
        if (isBlank(existing.getSummary())) {
            existing.setSummary(seed.getSummary());
        }
        if (isBlank(existing.getCoverImage())) {
            existing.setCoverImage(seed.getCoverImage());
        }
        if (isBlank(existing.getCategoryId())) {
            existing.setCategoryId(seed.getCategoryId());
        }
        if (isBlank(existing.getSourceBookId())) {
            existing.setSourceBookId(seed.getSourceBookId());
        }
        if (existing.getTotalChapters() <= 0) {
            existing.setTotalChapters(seed.getTotalChapters());
        }
        if (existing.getTotalPages() <= 0) {
            existing.setTotalPages(seed.getTotalPages());
        }
        // Keep seeded ranking demo data stable so Top tuan and Top thang differ clearly.
        existing.setViews(seed.getViews());
        existing.setAvgRating(seed.getAvgRating());
        existing.setCreatedAt(seed.getCreatedAt());
        return existing;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Book createBook(
        String title,
        String author,
        String description,
        String summary,
        String coverImage,
        String categoryId,
        int totalChapters,
        int totalPages,
        int views,
        double avgRating,
        int createdDaysAgo
    ) {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(createdDaysAgo);
        Book book = new Book();
        book.setSourceBookId("seed-" + title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""));
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setSummary(summary);
        book.setCoverImage(coverImage);
        book.setPublisher("");
        book.setPublishDate("");
        book.setStatus("Sẵn sàng");
        book.setCategoryId(categoryId);
        book.setCategories(List.of());
        book.setTags(List.of());
        book.setTotalChapters(totalChapters);
        book.setTotalPages(totalPages);
        book.setViews(views);
        book.setAvgRating(avgRating);
        book.setFeatured(false);
        book.setCreatedAt(createdAt);
        book.setUpdatedAt(createdAt);
        return book;
    }
}
