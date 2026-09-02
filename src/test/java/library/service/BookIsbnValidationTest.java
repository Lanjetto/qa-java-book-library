package library.service;

import library.exception.InvalidIsbnException;
import library.model.Author;
import library.model.Book;
import library.model.BookStatus;
import library.storage.InMemoryBookStorage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Параметризованные тесты валидации ISBN (@ParameterizedTest + @MethodSource/@ValueSource).
 * Правило сервиса: ISBN — ровно 13 или 10 цифр (см. BookService.ISBN_PATTERN).
 */
class BookIsbnValidationTest {

    private final BookService service = new BookService(new InMemoryBookStorage());

    private Book book(String isbn) {
        return new Book(null, isbn, "Title", new Author(null, "A", 1950),
                2020, new BigDecimal("100.00"), BookStatus.AVAILABLE);
    }

    static Stream<String> validIsbns() {
        // 13 цифр и 10 цифр (учебный ISBN-10 без контрольной суммы)
        return Stream.of("9780132350884", "9780134685991", "0132350887");
    }

    @ParameterizedTest(name = "валидный ISBN принят: {0}")
    @MethodSource("validIsbns")
    void acceptsValidIsbn(String isbn) {
        Book created = service.createBook(book(isbn));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getIsbn()).isEqualTo(isbn);
    }

    @ParameterizedTest(name = "невалидный ISBN отклонён: {0}")
    @ValueSource(strings = {"123", "978013235088X", "97801323508841"})
    @NullAndEmptySource
    void rejectsInvalidIsbn(String isbn) {
        assertThatThrownBy(() -> service.createBook(book(isbn)))
                .isInstanceOf(InvalidIsbnException.class);
    }
}
