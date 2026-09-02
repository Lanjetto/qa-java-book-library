package library.storage;

import library.model.Book;

import java.util.List;
import java.util.Optional;

/**
 * Абстракция хранилища книг.
 * Позволяет не зависеть от конкретной реализации (в памяти, БД, файл).
 */
public interface BookStorage {

    Book save(Book book);

    Optional<Book> findById(Long id);

    List<Book> findAll();

    void deleteById(Long id);
}
