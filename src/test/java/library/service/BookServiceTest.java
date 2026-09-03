package library.service;

import library.api.dto.UpdateBookRequest;
import library.exception.BookNotFoundException;
import library.exception.InvalidIsbnException;
import library.model.Book;
import library.model.BookStatus;
import library.repository.BookRepository;
import library.testdata.BookMother;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тест {@link BookService} с Mockito (Б14.4): БД — внешняя граница, поэтому репозиторий
 * мокается, Spring не поднимается. Быстро и стабильно; покрывает ветки сервиса, которые сквозной
 * HTTP-тест не достаёт (невалидный ISBN, пустой поиск, отсутствующая книга).
 *
 * <p>Строгие стабы (MockitoExtension по умолчанию): неиспользуемый стаб роняет тест — каждый тест
 * описывает ровно то поведение, которое проверяет.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @InjectMocks
    private BookService service;

    private Book cleanCode(Long id) {
        return BookMother.book(id, "9780132350884", "Чистый код", "Роберт Мартин",
                1952, 2008, "899.99", BookStatus.AVAILABLE);
    }

    // ---------- createBook ----------

    @Test
    @DisplayName("createBook: валидный ISBN → сохраняет книгу и возвращает её с id")
    void createBookWithValidIsbnSavesAndReturns() {
        Book input = cleanCode(null);
        when(repository.save(input)).thenAnswer(inv -> {
            Book saved = inv.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        Book created = service.createBook(input);

        assertThat(created.getId()).isEqualTo(7L);
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .extracting(Book::getIsbn, Book::getTitle)
                .containsExactly("9780132350884", "Чистый код");
    }

    @Test
    @DisplayName("createBook: ISBN из 10 цифр тоже валиден")
    void createBookAcceptsTenDigitIsbn() {
        Book input = BookMother.book(null, "0306406152", "Война и мир", "Лев Толстой",
                1828, 1869, "1000.00", BookStatus.AVAILABLE);
        when(repository.save(input)).thenReturn(input);

        service.createBook(input);

        verify(repository).save(input);
    }

    @Test
    @DisplayName("createBook: нецифровой/с дефисами/слишком короткий ISBN → InvalidIsbnException, без обращения к БД")
    void createBookWithInvalidIsbnThrows() {
        for (String bad : List.of("не-isbn", "978-0-13-235088-4", "12345", "97801323508844")) {
            Book input = cleanCode(null);
            input.setIsbn(bad);

            assertThatThrownBy(() -> service.createBook(input))
                    .isInstanceOf(InvalidIsbnException.class)
                    .hasMessageContaining(bad);
        }
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("createBook: null ISBN → InvalidIsbnException")
    void createBookWithNullIsbnThrows() {
        Book input = cleanCode(null);
        input.setIsbn(null);

        assertThatThrownBy(() -> service.createBook(input))
                .isInstanceOf(InvalidIsbnException.class);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("createBook: книга, сгенерированная Instancio (Б14), проходит валидацию ISBN после override")
    void createBookPersistsInstancioGeneratedBook() {
        // Instancio заполняет Book случайными данными; задаём то, что важно для нашей логики
        Book random = Instancio.of(Book.class)
                .set(field(Book::getIsbn), "9780132350884")
                .set(field(Book::getStatus), BookStatus.AVAILABLE)
                .create();
        when(repository.save(random)).thenReturn(random);

        Book saved = service.createBook(random);

        assertThat(saved).isSameAs(random);
        verify(repository).save(random);
    }

    // ---------- findById ----------

    @Test
    @DisplayName("findById: существующая книга возвращается")
    void findByIdReturnsBook() {
        when(repository.findById(1L)).thenReturn(Optional.of(cleanCode(1L)));

        assertThat(service.findById(1L))
                .extracting(Book::getId, Book::getTitle)
                .containsExactly(1L, "Чистый код");
    }

    @Test
    @DisplayName("findById: отсутствующая книга → BookNotFoundException с id в сообщении")
    void findByIdMissingThrows() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ---------- findAll / search ----------

    @Test
    @DisplayName("findAll возвращает все книги в порядке id")
    void findAllReturnsBooks() {
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(cleanCode(1L), cleanCode(2L)));

        assertThat(service.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("search: пустой/пробельный запрос → пустой список, БД не дёргаем")
    void searchWithBlankQueryReturnsEmptyWithoutDb() {
        assertThat(service.search(null)).isEmpty();
        assertThat(service.search("   ")).isEmpty();
        verify(repository, never()).findAllByOrderByIdAsc();
    }

    @Test
    @DisplayName("search: ищет по подстроке названия без учёта регистра")
    void searchFindsByTitleIgnoreCase() {
        Book clean = cleanCode(1L);
        clean.setTitle("Clean Code");
        when(repository.findAllByOrderByIdAsc())
                .thenReturn(List.of(clean, BookMother.book(2L, "9780134685991",
                        "Effective Java", "Джошуа Блох", 1961, 2018, "1200.00", BookStatus.AVAILABLE)));

        assertThat(service.search("CLEAN"))
                .singleElement()
                .extracting(Book::getTitle)
                .isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("search: ищет по имени автора (кириллица, без учёта регистра)")
    void searchFindsByAuthor() {
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(cleanCode(1L)));

        assertThat(service.search("мартин"))
                .singleElement()
                .extracting(Book::getAuthor)
                .extracting(author -> author.getFullName())
                .isEqualTo("Роберт Мартин");
    }

    @Test
    @DisplayName("search: книга без автора не роняет поиск (null-safe)")
    void searchDoesNotFailOnNullAuthor() {
        Book noAuthor = cleanCode(1L);
        noAuthor.setAuthor(null);
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(noAuthor));

        assertThat(service.search("clean")).isEmpty();   // ничего не упало, совпадений нет
    }

    @Test
    @DisplayName("search: без совпадений → пустой список")
    void searchReturnsEmptyWhenNoMatch() {
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(cleanCode(1L)));

        assertThat(service.search("zzz-не-существует")).isEmpty();
    }

    // ---------- updateBook ----------

    @Test
    @DisplayName("updateBook: меняет только переданные поля; при смене автора сохраняет год рождения")
    void updateBookChangesProvidedFieldsAndKeepsAuthorBirthYear() {
        Book existing = cleanCode(5L);
        existing.setStatus(BookStatus.RESERVED);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        UpdateBookRequest request = new UpdateBookRequest(
                "Чистый код (2-е изд.)", "Uncle Bob", 2009,
                new BigDecimal("1200.00"), BookStatus.SOLD);

        Book updated = service.updateBook(5L, request);

        assertThat(updated.getTitle()).isEqualTo("Чистый код (2-е изд.)");
        assertThat(updated.getAuthor().getFullName()).isEqualTo("Uncle Bob");
        // год рождения не передавали через REST — при смене имени автора сохраняем прежний
        assertThat(updated.getAuthor().getBirthYear()).isEqualTo(1952);
        assertThat(updated.getYear()).isEqualTo(2009);
        assertThat(updated.getPrice()).isEqualByComparingTo("1200.00");
        assertThat(updated.getStatus()).isEqualTo(BookStatus.SOLD);
    }

    @Test
    @DisplayName("updateBook: частичный PATCH (только status) не трогает остальные поля")
    void updateBookPartialPatch() {
        Book existing = cleanCode(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        Book updated = service.updateBook(5L, new UpdateBookRequest(null, null, null, null, BookStatus.RESERVED));

        assertThat(updated.getStatus()).isEqualTo(BookStatus.RESERVED);
        assertThat(updated.getTitle()).isEqualTo("Чистый код");
        assertThat(updated.getAuthor().getFullName()).isEqualTo("Роберт Мартин");
        assertThat(updated.getYear()).isEqualTo(2008);
    }

    @Test
    @DisplayName("updateBook: пустой запрос → книга сохраняется без изменений")
    void updateBookWithEmptyRequestKeepsBook() {
        Book existing = cleanCode(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        Book updated = service.updateBook(5L, new UpdateBookRequest(null, null, null, null, null));

        assertThat(updated).isEqualTo(existing);
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("updateBook: отсутствующая книга → BookNotFoundException, save не вызывается")
    void updateBookMissingThrowsAndDoesNotSave() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBook(5L, new UpdateBookRequest(null, null, null, null, BookStatus.SOLD)))
                .isInstanceOf(BookNotFoundException.class);
        verify(repository, never()).save(any());
    }

    // ---------- deleteById ----------

    @Test
    @DisplayName("deleteById: существующая книга удаляется")
    void deleteByIdExistingDeletes() {
        when(repository.findById(1L)).thenReturn(Optional.of(cleanCode(1L)));

        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteById: отсутствующая книга → BookNotFoundException, delete не вызывается")
    void deleteByIdMissingThrowsAndDoesNotDelete() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(BookNotFoundException.class);
        verify(repository, never()).deleteById(1L);
    }
}
