package library.service;

import library.exception.BookNotFoundException;
import library.exception.InvalidIsbnException;
import library.model.Author;
import library.model.Book;
import library.model.BookStatus;
import library.storage.InMemoryBookStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты BookService на чистом JUnit 5.
 * BookService зависит от абстракции BookStorage; здесь — реальная in-memory реализация.
 */
class BookServiceTest {

    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(new InMemoryBookStorage());
        seedCatalog();
    }

    private Book book(String isbn, String title, String authorName, BigDecimal price, BookStatus status) {
        return new Book(null, isbn, title,
                new Author(null, authorName, 1950), 2020, price, status);
    }

    private void seedCatalog() {
        service.createBook(book("9780132350884", "Clean Code", "Роберт Мартин",
                new BigDecimal("3000.00"), BookStatus.AVAILABLE));
        service.createBook(book("9780134685991", "Effective Java", "Джошуа Блох",
                new BigDecimal("5200.00"), BookStatus.RESERVED));
        service.createBook(book("9780321349606", "Java Concurrency", "Брайан Гоетц",
                new BigDecimal("4500.00"), BookStatus.AVAILABLE));
    }

    @Test
    @DisplayName("createBook с корректным ISBN сохраняет и присваивает id")
    void createBookAddsValidBook() {
        Book created = service.createBook(book("9780132778046", "New", "Автор",
                new BigDecimal("100.00"), BookStatus.AVAILABLE));

        assertEquals(4L, created.getId());
        assertEquals(4, service.findAll().size());
    }

    @Test
    @DisplayName("createBook с некорректным ISBN бросает InvalidIsbnException")
    void createBookRejectsInvalidIsbn() {
        Book bad = book("не-isbn", "Bad", "Автор",
                new BigDecimal("100.00"), BookStatus.AVAILABLE);

        assertThrows(InvalidIsbnException.class, () -> service.createBook(bad));
    }

    @Test
    @DisplayName("findById возвращает книгу, отсутствующую — BookNotFoundException")
    void findByIdThrowsWhenMissing() {
        assertEquals("Clean Code", service.findById(1L).getTitle());
        assertThrows(BookNotFoundException.class, () -> service.findById(999L));
    }

    @Test
    @DisplayName("search ищет по подстроке названия без учёта регистра")
    void searchFindsByTitleSubstring() {
        List<Book> found = service.search("clean");

        assertEquals(1, found.size());
        assertEquals("Clean Code", found.get(0).getTitle());
    }

    @Test
    @DisplayName("search ищет по подстроке имени автора")
    void searchFindsByAuthor() {
        List<Book> found = service.search("мартин");

        assertEquals(1, found.size());
        assertEquals("Clean Code", found.get(0).getTitle());
    }

    @Test
    @DisplayName("search с пустым запросом возвращает пустой список")
    void searchBlankReturnsEmpty() {
        assertTrue(service.search("  ").isEmpty());
    }

    @Test
    @DisplayName("findByStatus возвращает только книги нужного статуса")
    void findByStatusFiltersByStatus() {
        List<Book> available = service.findByStatus(BookStatus.AVAILABLE);

        assertEquals(2, available.size());
        assertTrue(available.stream().allMatch(b -> b.getStatus() == BookStatus.AVAILABLE));
    }

    @Test
    @DisplayName("topExpensive возвращает N самых дорогих по убыванию цены")
    void topExpensiveReturnsTopByPriceDesc() {
        List<Book> top2 = service.topExpensive(2);

        assertEquals(2, top2.size());
        assertEquals("Effective Java", top2.get(0).getTitle());
        assertEquals("Java Concurrency", top2.get(1).getTitle());
    }

    @Test
    @DisplayName("countByStatus группирует количество книг по статусам")
    void countByStatusGroupsCounts() {
        Map<BookStatus, Long> byStatus = service.countByStatus();

        assertEquals(2L, byStatus.get(BookStatus.AVAILABLE));
        assertEquals(1L, byStatus.get(BookStatus.RESERVED));
    }
}
