package library.service;

import library.exception.BookNotFoundException;
import library.exception.InvalidIsbnException;
import library.model.Book;
import library.model.BookStatus;
import library.storage.BookStorage;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Бизнес-логика библиотеки.
 * Зависит от абстракции {@link BookStorage}, а не от конкретной реализации.
 */
public class BookService {

    private static final Pattern ISBN_PATTERN = Pattern.compile("\\d{13}|\\d{10}");

    private final BookStorage storage;

    public BookService(BookStorage storage) {
        this.storage = storage;
    }

    public Book createBook(Book book) {
        if (book.getIsbn() == null || !ISBN_PATTERN.matcher(book.getIsbn()).matches()) {
            throw new InvalidIsbnException(book.getIsbn());
        }
        return storage.save(book);
    }

    public Book findById(Long id) {
        return storage.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public List<Book> findAll() {
        return storage.findAll();
    }

    /**
     * Поиск книг по подстроке в названии или авторе (без учёта регистра).
     * Демонстрация Stream API: filter + anyMatch на List<String>.
     */
    public List<Book> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.toLowerCase();
        return storage.findAll().stream()
                .filter(book -> book.getTitle().toLowerCase().contains(q)
                        || book.getAuthor().getFullName().toLowerCase().contains(q))
                .toList();
    }

    /**
     * Топ-N самых дорогих книг.
     * Демонстрация sorted() с Comparator и limit().
     */
    public List<Book> topExpensive(int n) {
        return storage.findAll().stream()
                .sorted(Comparator.comparing(Book::getPrice).reversed())
                .limit(n)
                .toList();
    }

    /**
     * Книги одного статуса.
     * Демонстрация filter() по enum.
     */
    public List<Book> findByStatus(BookStatus status) {
        return storage.findAll().stream()
                .filter(book -> book.getStatus() == status)
                .toList();
    }

    /**
     * Суммарная стоимость книг в продаже.
     * Демонстрация map() + reduce().
     */
    public BigDecimal totalPriceOf(BookStatus status) {
        return storage.findAll().stream()
                .filter(book -> book.getStatus() == status)
                .map(Book::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Группировка книг по статусу.
     * Демонстрация groupingBy() -> Map.
     */
    public Map<BookStatus, Long> countByStatus() {
        return storage.findAll().stream()
                .collect(Collectors.groupingBy(Book::getStatus, Collectors.counting()));
    }

    /**
     * Демонстрация Optional: книга по id либо запасное значение.
     */
    public Book findByIdOrFallback(Long id, Book fallback) {
        return storage.findById(id).orElse(fallback);
    }
}
