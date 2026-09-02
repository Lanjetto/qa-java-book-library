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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты InMemoryBookStorage на чистом JUnit 5 (без AssertJ — он появится в ветке t2).
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

        assertEquals(1L, a.getId());
        assertEquals(2L, b.getId());
    }

    @Test
    @DisplayName("save не перезаписывает уже заданный id")
    void saveKeepsExplicitId() {
        Book given = book(42L, "9780132350884");
        Book saved = storage.save(given);

        assertEquals(42L, saved.getId());
    }

    @Test
    @DisplayName("findById возвращает книгу, отсутствующую — empty")
    void findById() {
        storage.save(book(null, "9780132350884"));

        Optional<Book> present = storage.findById(1L);
        Optional<Book> absent = storage.findById(999L);

        assertTrue(present.isPresent());
        assertEquals("9780132350884", present.orElseThrow().getIsbn());
        assertFalse(absent.isPresent());
    }

    @Test
    @DisplayName("findAll сохраняет порядок добавления")
    void findAllKeepsInsertionOrder() {
        storage.save(book(null, "9780132350884"));
        storage.save(book(null, "9780134685991"));

        List<Book> all = storage.findAll();
        assertEquals(2, all.size());
        assertEquals("9780132350884", all.get(0).getIsbn());
        assertEquals("9780134685991", all.get(1).getIsbn());
    }

    @Test
    @DisplayName("deleteById удаляет книгу и убирает ISBN из множества")
    void deleteByIdRemovesBookAndIsbn() {
        storage.save(book(null, "9780132350884"));
        storage.deleteById(1L);

        assertTrue(storage.findAll().isEmpty());
        assertTrue(storage.getUniqueIsbns().isEmpty());
    }

    @Test
    @DisplayName("одинаковые ISBN не дублируются во множестве")
    void uniqueIsbnsContainsEachIsbnOnce() {
        storage.save(book(1L, "9780132350884"));
        storage.save(book(2L, "9780132350884"));

        Set<String> isbns = storage.getUniqueIsbns();
        assertEquals(1, isbns.size());
        assertEquals(Set.of("9780132350884"), isbns);
    }
}
