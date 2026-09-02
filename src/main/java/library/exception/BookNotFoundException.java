package library.exception;

/**
 * Книга с запрошенным id не найдена.
 * Unchecked (RuntimeException): вызывающий может не декларировать throws.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Книга с id=" + id + " не найдена");
    }
}
