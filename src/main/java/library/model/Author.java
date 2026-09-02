package library.model;

import java.util.Objects;

/**
 * Автор книги.
 */
public class Author {

    private Long id;
    private String fullName;
    private int birthYear;

    public Author() {
    }

    public Author(Long id, String fullName, int birthYear) {
        this.id = id;
        this.fullName = fullName;
        this.birthYear = birthYear;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Author author = (Author) o;
        return birthYear == author.birthYear
                && Objects.equals(id, author.id)
                && Objects.equals(fullName, author.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fullName, birthYear);
    }

    @Override
    public String toString() {
        return "Author{id=" + id
                + ", fullName='" + fullName + '\''
                + ", birthYear=" + birthYear
                + '}';
    }
}
