package library.service;

import library.api.dto.UpdateBookRequest;
import library.exception.BookNotFoundException;
import library.exception.InvalidIsbnException;
import library.model.Author;
import library.model.Book;
import library.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Бизнес-логика библиотеки на Spring Data JPA (с ветки t4).
 * Вместо in-memory хранилища внедряется {@link BookRepository}; «толстые» учебные методы
 * Дня 1 (topExpensive/totalPriceOf/countByStatus/findByStatus/findByIdOrFallback) убраны —
 * они остались в истории ветки {@code start} и не нужны REST-слою.
 */
@Service
public class BookService {

    private static final Pattern ISBN_PATTERN = Pattern.compile("\\d{13}|\\d{10}");

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public Book createBook(Book book) {
        if (book.getIsbn() == null || !ISBN_PATTERN.matcher(book.getIsbn()).matches()) {
            throw new InvalidIsbnException(book.getIsbn());
        }
        return repository.save(book);
    }

    public Book findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public List<Book> findAll() {
        return repository.findAllByOrderByIdAsc();
    }

    /**
     * Поиск по подстроке в названии или имени автора (без учёта регистра).
     * Оставлен фильтром по списку на уровне Java — чтобы не тянуть в SQL полнотекст;
     * при росте данных такой поиск выносят в репозиторий (@Query/LIKE) или полнотекстовый индекс.
     */
    public List<Book> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.toLowerCase();
        return repository.findAllByOrderByIdAsc().stream()
                .filter(book -> book.getTitle().toLowerCase().contains(q)
                        || (book.getAuthor() != null
                            && book.getAuthor().getFullName() != null
                            && book.getAuthor().getFullName().toLowerCase().contains(q)))
                .toList();
    }

    /**
     * PATCH-обновление книги: меняются только не-null поля запроса.
     * Автор в REST-контракте — строка (полное имя), поэтому при смене автора
     * год рождения сохраняем прежним.
     */
    public Book updateBook(Long id, UpdateBookRequest request) {
        Book existing = findById(id);
        if (request.title() != null) {
            existing.setTitle(request.title());
        }
        if (request.author() != null) {
            Author current = existing.getAuthor();
            existing.setAuthor(new Author(
                    request.author(),
                    current == null ? 0 : current.getBirthYear()));
        }
        if (request.year() != null) {
            existing.setYear(request.year());
        }
        if (request.price() != null) {
            existing.setPrice(request.price());
        }
        if (request.status() != null) {
            existing.setStatus(request.status());
        }
        return repository.save(existing);
    }

    /**
     * Удаление книги по id. Если книги нет — BookNotFoundException (404 на HTTP-слое).
     */
    public void deleteById(Long id) {
        findById(id);
        repository.deleteById(id);
    }
}
