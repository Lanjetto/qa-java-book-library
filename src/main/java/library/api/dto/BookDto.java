package library.api.dto;

import library.model.Book;
import library.model.BookStatus;

import java.math.BigDecimal;

/**
 * Ответ API «книга». Не отдаём наружу доменную модель напрямую:
 * наружу идёт плоский DTO, где автор — строка (полное имя).
 */
public record BookDto(Long id, String isbn, String title, String author,
                      int year, BigDecimal price, BookStatus status) {

    public static BookDto from(Book book) {
        return new BookDto(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor() == null ? null : book.getAuthor().getFullName(),
                book.getYear(),
                book.getPrice(),
                book.getStatus());
    }
}
