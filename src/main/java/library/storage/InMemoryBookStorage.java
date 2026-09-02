package library.storage;

import library.model.Book;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Реализация хранилища книг в памяти на коллекциях Java.
 * - Map: поиск по id за O(1)
 * - List: сохранение порядка добавления
 * - Set: уникальные ISBN без дублей
 */
public class InMemoryBookStorage implements BookStorage {

    private final Map<Long, Book> booksById = new HashMap<>();
    private final List<Book> booksInOrder = new ArrayList<>();
    private final Set<String> uniqueIsbns = new LinkedHashSet<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public Book save(Book book) {
        if (book.getId() == null) {
            book.setId(idSequence.getAndIncrement());
        }
        uniqueIsbns.add(book.getIsbn());
        booksById.put(book.getId(), book);
        booksInOrder.add(book);
        return book;
    }

    @Override
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(booksById.get(id));
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(booksInOrder);
    }

    @Override
    public void deleteById(Long id) {
        Book removed = booksById.remove(id);
        if (removed != null) {
            booksInOrder.remove(removed);
            uniqueIsbns.remove(removed.getIsbn());
        }
    }

    public Set<String> getUniqueIsbns() {
        return new LinkedHashSet<>(uniqueIsbns);
    }
}
