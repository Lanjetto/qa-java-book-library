package library.storage;

import library.model.Author;
import library.model.Book;
import library.model.BookStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты InMemoryBookStorage. С ветки t2 — на AssertJ (fluent-assertions).
 * Проверяем поведение коллекций: Map (id->Book), List (порядок), Set (уникальные ISBN).
 */
class BookStorageTest {

    private InMemoryBookStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryBookStorage();
    }

    private Book book(Long id, String isbn) {
        return new Book(id, isbn, "Title " + isbn,
                new Author(null, "Author", 1950), 2000,
                new BigDecimal("100.00"), BookStatus.AVAILABLE);
    }

    @Test
    @DisplayName("save присваивает id последовательно: 1, 2, 3…")
    void saveAssignsSequentialIds() {
        Book a = storage.save(book(null, "9780132350884"));
        Book b = storage.save(book(null, "9780134685991"));

        assertThat(a.getId()).isEqualTo(1L);
        assertThat(b.getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("save не перезаписывает уже заданный id")
    void saveKeepsExplicitId() {
        Book saved = storage.save(book(42L, "9780132350884"));

        assertThat(saved.getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("findById возвращает книгу, отсутствующую — empty")
    void findById() {
        storage.save(book(null, "9780132350884"));

        Optional<Book> present = storage.findById(1L);
        Optional<Book> absent = storage.findById(999L);

        assertThat(present).isPresent();
        assertThat(present).get().extracting(Book::getIsbn).isEqualTo("9780132350884");
        assertThat(absent).isEmpty();
    }

    @Test
    @DisplayName("findAll сохраняет порядок добавления")
    void findAllKeepsInsertionOrder() {
        storage.save(book(null, "9780132350884"));
        storage.save(book(null, "9780134685991"));

        List<Book> all = storage.findAll();
        assertThat(all).hasSize(2)
                .extracting(Book::getIsbn)
                .containsExactly("9780132350884", "9780134685991");
    }

    @Test
    @DisplayName("deleteById удаляет книгу и убирает ISBN из множества")
    void deleteByIdRemovesBookAndIsbn() {
        storage.save(book(null, "9780132350884"));
        storage.deleteById(1L);

        assertThat(storage.findAll()).isEmpty();
        assertThat(storage.getUniqueIsbns()).isEmpty();
    }

    @Test
    @DisplayName("одинаковые ISBN не дублируются во множестве")
    void uniqueIsbnsContainsEachIsbnOnce() {
        storage.save(book(1L, "9780132350884"));
        storage.save(book(2L, "9780132350884"));

        assertThat(storage.getUniqueIsbns())
                .containsExactly("9780132350884");
    }
}
