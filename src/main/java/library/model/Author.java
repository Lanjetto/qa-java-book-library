package library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Автор книги — встраиваемое значение (@Embeddable), а не отдельная сущность.
 * С ветки t4 автор хранится колонками {@code author_full_name}/{@code author_birth_year}
 * прямо в таблице {@code books}: у книги нет внешнего ключа {@code author_id}, а у автора
 * нет собственного id (в in-memory фазе id был нужен, в БД-схеме — нет).
 * equals/hashCode/toString и аксессоры — на Lombok (@Data).
 */
@Data
@NoArgsConstructor
@Embeddable
public class Author {

    @Column(name = "author_full_name")
    private String fullName;

    @Column(name = "author_birth_year")
    private int birthYear;

    public Author(String fullName, int birthYear) {
        this.fullName = fullName;
        this.birthYear = birthYear;
    }
}
