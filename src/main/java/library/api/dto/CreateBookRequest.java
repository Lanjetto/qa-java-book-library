package library.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import library.model.Author;
import library.model.Book;
import library.model.BookStatus;

import java.math.BigDecimal;

/**
 * Тело POST /api/books. Валидация — bean-валидацией (jakarta.validation),
 * контроллер помечен @Valid. Автор передаётся строкой (полное имя); год рождения
 * в REST-контракте не передаётся и проставляется как 0 (см. {@link Author}).
 */
public record CreateBookRequest(
        @NotBlank String isbn,
        @NotBlank String title,
        @NotBlank String author,
        @NotNull Integer year,
        @NotNull BigDecimal price,
        @NotNull BookStatus status) {

    public Book toBook() {
        return new Book(null, isbn, title, new Author(author, 0),
                year, price, status);
    }
}
