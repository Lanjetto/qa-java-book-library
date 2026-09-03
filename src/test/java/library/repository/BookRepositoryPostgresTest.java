package library.repository;

import library.model.Author;
import library.model.Book;
import library.model.BookStatus;
import library.testcontainers.AbstractPostgresIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тот же {@link BookRepositoryTest} (ветка t4), но на **настоящем Postgres** (Testcontainers, Б15).
 * {@code @DataJpaTest} по умолчанию подменяет DataSource встроенной H2 — это отключаем
 * {@code @AutoConfigureTestDatabase(replace = NONE)}: используем контейнер из {@link AbstractPostgresIT}.
 * Liquibase применяется в контейнер, {@code ddl-auto: validate} — как в проде.
 *
 * <p>Диалект Postgres не эмулируется H2: {@code findByStatus}/{@code findByTitleContainingIgnoreCase}
 * (ILIKE-семантика регистра) должны быть зелёными на реальной БД. Каждый тест в транзакции — откатывается.
 */
@Tag("docker")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryPostgresTest extends AbstractPostgresIT {

    @Autowired
    private BookRepository repository;

    @Autowired
    private TestEntityManager em;

    private Book book(String isbn, String title, String authorName, int birthYear, BookStatus status) {
        Book b = new Book();
        b.setIsbn(isbn);
        b.setTitle(title);
        b.setAuthor(new Author(authorName, birthYear));
        b.setYear(2020);
        b.setPrice(new BigDecimal("100.00"));
        b.setStatus(status);
        return b;
    }

    @Test
    @DisplayName("findByStatus работает на настоящем Postgres")
    void findByStatusOnRealPostgres() {
        repository.save(book("9780132350884", "Clean Code", "Роберт Мартин", 1952, BookStatus.AVAILABLE));
        repository.save(book("9780134685991", "Effective Java", "Джошуа Блох", 1961, BookStatus.RESERVED));

        assertThat(repository.findByStatus(BookStatus.RESERVED))
                .singleElement()
                .extracting(Book::getTitle)
                .isEqualTo("Effective Java");
    }

    @Test
    @DisplayName("findByTitleContainingIgnoreCase не зависит от регистра (как и на H2)")
    void findByTitleIgnoreCaseOnRealPostgres() {
        repository.save(book("9785026034478", "Война и мир", "Лев Толстой", 1828, BookStatus.AVAILABLE));

        assertThat(repository.findByTitleContainingIgnoreCase("ВОЙНА"))
                .singleElement()
                .extracting(Book::getTitle)
                .isEqualTo("Война и мир");
    }

    @Test
    @DisplayName("findByIsbn на настоящем Postgres; отсутствующей нет")
    void findByIsbnOnRealPostgres() {
        repository.save(book("9780132350884", "Clean Code", "Роберт Мартин", 1952, BookStatus.AVAILABLE));

        Optional<Book> present = repository.findByIsbn("9780132350884");
        Optional<Book> absent = repository.findByIsbn("0000000000000");

        assertThat(present).isPresent()
                .get().extracting(Book::getTitle).isEqualTo("Clean Code");
        assertThat(absent).isEmpty();
    }
}
