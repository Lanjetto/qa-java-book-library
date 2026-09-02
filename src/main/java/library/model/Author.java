package library.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Автор книги.
 * equals/hashCode/toString и аксессоры — на Lombok (@Data).
 */
@Data
@NoArgsConstructor
public class Author {

    private Long id;
    private String fullName;
    private int birthYear;

    public Author(Long id, String fullName, int birthYear) {
        this.id = id;
        this.fullName = fullName;
        this.birthYear = birthYear;
    }
}
