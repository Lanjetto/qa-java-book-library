package library.storage;

import library.model.Book;
import org.springframework.stereotype.Repository;

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
 *
 * Spring-бин (@Repository): с t3 внедряется в BookService через BookStorage.
 */
@Repository
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

        // Новая книга — добавляем в конец; обновление существующего id — заменяем на месте,
        // чтобы update (PATCH) не плодил дубли в List и не менял порядок.
        int index = indexOfId(book.getId());
        if (index < 0) {
            booksInOrder.add(book);
        } else {
            booksInOrder.set(index, book);
        }
        return book;
    }

    private int indexOfId(Long id) {
        for (int i = 0; i < booksInOrder.size(); i++) {
            if (booksInOrder.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
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
