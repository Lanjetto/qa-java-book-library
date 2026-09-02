package library.service;

import library.api.dto.UpdateBookRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты BookService на AssertJ.
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

        assertThat(created.getId()).isEqualTo(4L);
        assertThat(service.findAll()).hasSize(4);
    }

    @Test
    @DisplayName("createBook с некорректным ISBN бросает InvalidIsbnException")
    void createBookRejectsInvalidIsbn() {
        Book bad = book("не-isbn", "Bad", "Автор",
                new BigDecimal("100.00"), BookStatus.AVAILABLE);

        assertThatThrownBy(() -> service.createBook(bad))
                .isInstanceOf(InvalidIsbnException.class)
                .hasMessageContaining("ISBN");
    }

    @Test
    @DisplayName("findById возвращает книгу, отсутствующую — BookNotFoundException")
    void findByIdThrowsWhenMissing() {
        assertThat(service.findById(1L).getTitle()).isEqualTo("Clean Code");

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("search ищет по подстроке названия без учёта регистра")
    void searchFindsByTitleSubstring() {
        List<Book> found = service.search("clean");

        assertThat(found).singleElement()
                .extracting(Book::getTitle).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("search ищет по подстроке имени автора")
    void searchFindsByAuthor() {
        List<Book> found = service.search("мартин");

        assertThat(found).singleElement()
                .extracting(Book::getTitle).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("search с пустым запросом возвращает пустой список")
    void searchBlankReturnsEmpty() {
        assertThat(service.search("  ")).isEmpty();
    }

    @Test
    @DisplayName("findByStatus возвращает только книги нужного статуса")
    void findByStatusFiltersByStatus() {
        List<Book> available = service.findByStatus(BookStatus.AVAILABLE);

        assertThat(available).hasSize(2)
                .extracting(Book::getStatus)
                .containsOnly(BookStatus.AVAILABLE);
    }

    @Test
    @DisplayName("topExpensive возвращает N самых дорогих по убыванию цены")
    void topExpensiveReturnsTopByPriceDesc() {
        List<Book> top2 = service.topExpensive(2);

        assertThat(top2).extracting(Book::getTitle)
                .containsExactly("Effective Java", "Java Concurrency");
    }

    @Test
    @DisplayName("countByStatus группирует количество книг по статусам")
    void countByStatusGroupsCounts() {
        Map<BookStatus, Long> byStatus = service.countByStatus();

        assertThat(byStatus)
                .containsEntry(BookStatus.AVAILABLE, 2L)
                .containsEntry(BookStatus.RESERVED, 1L);
    }

    @Test
    @DisplayName("totalPriceOf суммирует стоимость книг одного статуса")
    void totalPriceOfSumsByStatus() {
        // Clean Code (3000) + Java Concurrency (4500) = 7500
        assertThat(service.totalPriceOf(BookStatus.AVAILABLE))
                .isEqualByComparingTo("7500.00");
    }

    @Test
    @DisplayName("findByIdOrFallback: найденная книга или запасная")
    void findByIdOrFallbackReturnsBookOrFallback() {
        Book fallback = book("9780132778046", "Fallback", "Автор",
                new BigDecimal("1.00"), BookStatus.SOLD);

        assertThat(service.findByIdOrFallback(1L, fallback).getTitle())
                .isEqualTo("Clean Code");
        assertThat(service.findByIdOrFallback(999L, fallback)).isSameAs(fallback);
    }

    @Test
    @DisplayName("updateBook меняет только переданные поля")
    void updateBookChangesOnlyProvidedFields() {
        Book updated = service.updateBook(1L,
                new UpdateBookRequest(null, null, null, new BigDecimal("3500.00"), BookStatus.SOLD));

        assertThat(updated.getTitle()).isEqualTo("Clean Code");
        assertThat(updated.getPrice()).isEqualByComparingTo("3500.00");
        assertThat(updated.getStatus()).isEqualTo(BookStatus.SOLD);
        assertThat(updated.getAuthor().getFullName()).isEqualTo("Роберт Мартин");
    }

    @Test
    @DisplayName("updateBook отсутствующей книги — BookNotFoundException")
    void updateBookThrowsWhenMissing() {
        assertThatThrownBy(() -> service.updateBook(999L, new UpdateBookRequest(null, null, null, null, null)))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("deleteById удаляет книгу, повторный findById бросает")
    void deleteByIdRemovesBook() {
        service.deleteById(1L);

        assertThat(service.findAll()).hasSize(2);
        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("deleteById отсутствующей книги — BookNotFoundException")
    void deleteByIdThrowsWhenMissing() {
        assertThatThrownBy(() -> service.deleteById(999L))
                .isInstanceOf(BookNotFoundException.class);
    }
}
