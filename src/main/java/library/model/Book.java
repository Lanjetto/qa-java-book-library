package library.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Книга библиотеки.
 * Геттеры/сеттеры/equals/hashCode/toString генерирует Lombok (@Data) —
 * вручную написанный бойлерплейт удалён при рефакторинге в ветке t2.
 */
@Data
@NoArgsConstructor
public class Book {

    private Long id;
    private String isbn;
    private String title;
    private Author author;
    private int year;
    private BigDecimal price;
    private BookStatus status;

    public Book(Long id, String isbn, String title, Author author,
                int year, BigDecimal price, BookStatus status) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.year = year;
        this.price = price;
        this.status = status;
    }
}
