package library.api.dto;

import library.model.BookStatus;

import java.math.BigDecimal;

/**
 * Тело PATCH /api/books/{id}. Все поля необязательны: обновляются только не-null.
 */
public record UpdateBookRequest(
        String title,
        String author,
        Integer year,
        BigDecimal price,
        BookStatus status) {
}
