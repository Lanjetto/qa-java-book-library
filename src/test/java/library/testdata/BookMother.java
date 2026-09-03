package library.testdata;

import library.api.dto.CreateBookRequest;
import library.model.Author;
import library.model.Book;
import library.model.BookStatus;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Фабрика тестовых данных (паттерн Object Mother, Б14).
 *
 * <p>Готовые {@link CreateBookRequest} удобно слать в REST Assured — тело сериализуется как есть.
 * Доменный {@link Book} для unit-тестов сервиса (репозиторий замокан) получаем либо из запроса
 * через {@code toBook()}, либо напрямую из {@link #book(Long, String, String, String, int, int, String, BookStatus)}.
 *
 * <p>ISBN: реальные книги задаются фиксированными ISBN (только для юнитов/слайсов — там нет БД
 * с ограничением уникальности), а для интеграционных тестов против «живой» БД используем
 * {@link #uniqueIsbn()} — он не повторяется между запусками (данные в Postgres сохраняются).
 */
public final class BookMother {

    private static final AtomicLong SEQ = new AtomicLong(1);

    private BookMother() {
    }

    public static CreateBookRequest cleanCode() {
        return request("9780132350884", "Чистый код", "Роберт Мартин", 2008, "899.99");
    }

    public static CreateBookRequest effectiveJava() {
        return request("9780134685991", "Effective Java", "Джошуа Блох", 2018, "1200.00");
    }

    /** Книга с уникальным ISBN — для тестов, создающих книги через API против реальной БД. */
    public static CreateBookRequest unique() {
        return request(uniqueIsbn(), "Книга №" + SEQ.getAndIncrement(), "Автор N", 2024, "500.00");
    }

    /** Книга с произвольным (заданным вручную) ISBN. */
    public static CreateBookRequest withIsbn(String isbn) {
        return new CreateBookRequest(isbn, "Книга по ISBN", "Автор", 2024,
                new BigDecimal("100.00"), BookStatus.AVAILABLE);
    }

    /**
     * 13-значный ISBN (978 + 10 цифр), уникальный и между запусками одного прогона,
     * и между разными запусками одной и той же БД (случайные цифры из UUID).
     */
    public static String uniqueIsbn() {
        long low = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000_000L;
        return "978" + String.format("%010d", low);
    }

    /** Доменный Book из готового запроса + id (для unit-тестов сервиса). */
    public static Book book(Long id, CreateBookRequest request) {
        Book book = request.toBook();
        book.setId(id);
        return book;
    }

    /** Доменный Book напрямую (когда нужен автор с годом рождения, не проходящий через REST). */
    public static Book book(Long id, String isbn, String title, String author,
                            int birthYear, int year, String price, BookStatus status) {
        Book book = new Book();
        book.setId(id);
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(new Author(author, birthYear));
        book.setYear(year);
        book.setPrice(new BigDecimal(price));
        book.setStatus(status);
        return book;
    }

    private static CreateBookRequest request(String isbn, String title, String author,
                                             int year, String price) {
        return new CreateBookRequest(isbn, title, author, year, new BigDecimal(price),
                BookStatus.AVAILABLE);
    }
}
