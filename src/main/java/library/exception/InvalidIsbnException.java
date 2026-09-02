package library.exception;

/**
 * Некорректный ISBN у книги.
 * Unchecked. Наследует IllegalArgumentException — контракт «невалидный аргумент».
 */
public class InvalidIsbnException extends IllegalArgumentException {

    public InvalidIsbnException(String isbn) {
        super("Некорректный ISBN: '" + isbn + "'. Ожидается 10 или 13 цифр");
    }
}
