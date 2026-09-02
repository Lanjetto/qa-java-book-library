package library.repository;

import library.model.Book;
import library.model.BookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA репозиторий книг. Реализацию генерирует Spring по имени метода
 * (derived queries): {@code findByIsbn} превращается в SQL {@code where isbn = ?},
 * {@code findByTitleContainingIgnoreCase} — в {@code where lower(title) like %...%}.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByStatus(BookStatus status);

    /**
     * Стабильный порядок списка — по возрастанию id (в память-хранилище порядок был
     * порядком добавления, а у СУБД его нет без ORDER BY).
     */
    List<Book> findAllByOrderByIdAsc();
}
