package library.repository;

import library.model.Author;
import library.model.Book;
import library.model.BookStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Срез JPA-слоя (@DataJpaTest, Б12): поднимается только репозиторий/сущности/EntityManager —
 * без сервисов и контроллеров. Каждый тест выполняется в транзакции и откатывается после,
 * поэтому данные не «протекают» между тестами.
 *
 * Профиль test → H2 (MODE=PostgreSQL); схему (Liquibase) и сущности проверяем локально.
 * Настоящий Postgres — Testcontainers в Б15 (ветка t6).
 */
@DataJpaTest
@ActiveProfiles("test")
class BookRepositoryTest {

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
    @DisplayName("save присваивает id, сохраняет автора встроенными колонками и проставляет createdAt")
    void saveAssignsIdPersistsEmbeddedAuthorAndSetsCreatedAt() {
        Book saved = repository.save(
                book("9780132350884", "Clean Code", "Роберт Мартин", 1952, BookStatus.AVAILABLE));

        // сбрасываем контекст, чтобы читать из БД заново, а не из кэша первого уровня
        em.flush();
        em.clear();

        Book reloaded = em.find(Book.class, saved.getId());
        assertThat(reloaded.getTitle()).isEqualTo("Clean Code");
        assertThat(reloaded.getAuthor().getFullName()).isEqualTo("Роберт Мартин");
        assertThat(reloaded.getAuthor().getBirthYear()).isEqualTo(1952);
        // createdAt не задавали вручную — его проставил @PrePersist
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByIsbn возвращает книгу по ISBN; отсутствующую — empty")
    void findByIsbnFindsBook() {
        repository.save(book("9780134685991", "Effective Java", "Джошуа Блох", 1961, BookStatus.AVAILABLE));

        Optional<Book> present = repository.findByIsbn("9780134685991");
        Optional<Book> absent = repository.findByIsbn("0000000000000");

        assertThat(present).isPresent()
                .get().extracting(Book::getTitle).isEqualTo("Effective Java");
        assertThat(absent).isEmpty();
    }

    @Test
    @DisplayName("findByTitleContainingIgnoreCase ищет без учёта регистра")
    void findByTitleContainingIgnoreCaseIsCaseInsensitive() {
        repository.save(book("9785026034478", "Война и мир", "Лев Толстой", 1828, BookStatus.AVAILABLE));

        assertThat(repository.findByTitleContainingIgnoreCase("ВОЙНА"))
                .singleElement()
                .extracting(Book::getTitle)
                .isEqualTo("Война и мир");
    }

    @Test
    @DisplayName("findByStatus возвращает только книги нужного статуса")
    void findByStatusFiltersByStatus() {
        repository.save(book("9780132350884", "Clean Code", "Роберт Мартин", 1952, BookStatus.AVAILABLE));
        repository.save(book("9780134685991", "Effective Java", "Джошуа Блох", 1961, BookStatus.RESERVED));

        assertThat(repository.findByStatus(BookStatus.RESERVED))
                .singleElement()
                .extracting(Book::getTitle)
                .isEqualTo("Effective Java");
    }

    @Test
    @DisplayName("findAllByOrderByIdAsc возвращает книги в порядке id")
    void findAllByOrderByIdAscReturnsByIdOrder() {
        Book first = repository.save(book("9780132350884", "Clean Code", "Роберт Мартин", 1952, BookStatus.AVAILABLE));
        Book second = repository.save(book("9780134685991", "Effective Java", "Джошуа Блох", 1961, BookStatus.AVAILABLE));

        List<Book> all = repository.findAllByOrderByIdAsc();

        assertThat(all).extracting(Book::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("delete удаляет книгу по id")
    void deleteRemovesBook() {
        Book saved = repository.save(book("9780132350884", "Clean Code", "Роберт Мартин", 1952, BookStatus.AVAILABLE));

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}
