package library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Книга библиотеки — JPA-сущность (с ветки t4 живёт в БД; схемой управляет Liquibase,
 * Hibernate ничего не создаёт сам — {@code ddl-auto: validate}).
 * Геттеры/сеттеры/equals/hashCode/toString — на Lombok (@Data).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "isbn", nullable = false, unique = true, length = 13)
    private String isbn;

    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Автор — встроенное значение: колонки author_full_name/author_birth_year
     * в той же таблице books (канон домена, Gate 0).
     */
    @Embedded
    private Author author;

    // Колонка названа publication_year, а не year: YEAR — зарезервированное слово в H2
    // (и ряде СУБД), на Postgres допустимо, но переносимый вариант — publication_year.
    @Column(name = "publication_year")
    private int year;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BookStatus status;

    /**
     * Момент добавления книги. В БД — TIMESTAMPTZ (абсолютный момент в UTC), а не
     * «календарное» время без пояса. Проставляется автоматически при persist (@PrePersist);
     * расхождение времени между БД/JSON/тестами разбираем в Б14.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

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
