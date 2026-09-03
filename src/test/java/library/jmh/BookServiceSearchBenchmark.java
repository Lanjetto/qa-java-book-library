package library.jmh;

import library.messaging.BookEventPublisher;
import library.model.Book;
import library.model.BookStatus;
import library.repository.BookRepository;
import library.service.BookService;
import library.testdata.BookMother;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Микро-бенчмарк (JMH, t10/Б21) {@link BookService#search(String)}.
 *
 * <p>Что делает JMH сам: греет код до стабильного JIT (warmup), форком изолирует от соседних
 * процессов и JIT-состояния, результат (возвращённое значение) считает «использованным» —
 * не даёт JIT выкинуть вычисление. {@code @BenchmarkMode(AverageTime)} + {@code @OutputTimeUnit}
 * (микросекунды), {@code @Warmup/@Fork} — короткие, чтобы прогон шёл быстро.
 *
 * <p>Важно (Б21): JMH ≠ нагрузочный тест. Цифры микро-бенча (наносекунды на вызов метода)
 * нельзя переносить на всё приложение — здесь сравниваются только варианты внутри одного метода.
 *
 * <p>Запуск: {@code ./gradlew jmhRun} (короткий прогон; настройки в build.gradle).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class BookServiceSearchBenchmark {

    /** Набор «каталога»: 500 книг в памяти (репозиторий замокан — бенчим только логику search). */
    private BookService service;
    private List<Book> catalogue;

    @Setup
    public void setUp() {
        catalogue = IntStream.range(0, 500)
                .mapToObj(i -> BookMother.book(
                        (long) i,
                        String.format("978%010d", i),
                        "Книга про Java и автоматизацию " + i,
                        "Автор N" + i,
                        1970 + (i % 40),
                        2000 + (i % 20),
                        "100.00",
                        BookStatus.AVAILABLE))
                .toList();

        BookRepository repository = mock(BookRepository.class);
        when(repository.findAllByOrderByIdAsc()).thenReturn(catalogue);
        service = new BookService(repository, mock(BookEventPublisher.class));
    }

    /** Поиск, находящий книгу в конце каталога (совпадение только у последней — вынуждает пройти весь список). */
    @Benchmark
    public List<Book> searchMatchLast() {
        return service.search("n499");   // автор последней книги — «Автор N499»
    }

    /** Поиск без совпадений — полный проход по 500 книгам. */
    @Benchmark
    public List<Book> searchNoMatch() {
        return service.search("zzzz-no-such-book-zzzz");
    }
}
