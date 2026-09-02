package library.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Книга библиотеки.
 */
public class Book {

    private Long id;
    private String isbn;
    private String title;
    private Author author;
    private int year;
    private BigDecimal price;
    private BookStatus status;

    public Book() {
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BookStatus getStatus() {
        return status;
    }

    public void setStatus(BookStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Book book = (Book) o;
        return year == book.year
                && Objects.equals(id, book.id)
                && Objects.equals(isbn, book.isbn)
                && Objects.equals(title, book.title)
                && Objects.equals(author, book.author)
                && Objects.equals(price, book.price)
                && status == book.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, isbn, title, author, year, price, status);
    }

    @Override
    public String toString() {
        return "Book{id=" + id
                + ", isbn='" + isbn + '\''
                + ", title='" + title + '\''
                + ", author=" + author
                + ", year=" + year
                + ", price=" + price
                + ", status=" + status
                + '}';
    }
}
