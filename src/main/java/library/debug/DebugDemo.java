package library.debug;

import library.model.Author;
import library.model.Book;
import library.model.BookStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Реалистичный сценарий для отладки: импорт каталога книг из "файла" (сырых строк).
 *
 * В коде намеренно заложены три бага, чтобы отработать приёмы IntelliJ IDEA:
 * 1) NumberFormatException — цена "не число" в строке 4 (parseLines);
 * 2) NullPointerException — у строки 5 нет автора, а код зовёт getFullName() (normalizeAuthors);
 * 3) IndexOutOfBoundsException — в printCatalog цикл до size() включительно.
 *
 * Если не чинить баги, каждый следующий не будет достигнут: первым упадёт parseLines.
 * Поэтому отладка идёт по кругу: нашёл баг -> исправил -> перезапустил -> нашёл следующий.
 */
public class DebugDemo {

    public static void importCatalog() {
        List<String> lines = rawFileLines();
        List<Book> books = parseLines(lines);      // баг 1: NumberFormatException
        normalizeAuthors(books);                   // баг 2: NullPointerException
        printCatalog(books);                       // баг 3: IndexOutOfBoundsException
    }

    /** "Файл" каталога. Формат строки: ISBN|название|автор|год|цена. */
    private static List<String> rawFileLines() {
        return List.of(
                "9780132350884|Clean Code|Роберт Мартин|2008|3000.00",
                "9780321349606|Java Concurrency in Practice|Брайан Гоетц|2006|4500.00",
                "9780134685991|Effective Java|Джошуа Блох|2018|5200.00",
                "9780132778046|BROKEN|Роберт Мартин|2020|не число",
                "9780132778047|No Author||2021|1000.00"
        );
    }

    private static List<Book> parseLines(List<String> lines) {
        List<Book> books = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split("\\|");
            Book book = new Book();
            book.setIsbn(parts[0]);
            book.setTitle(parts[1]);
            if (!parts[2].isBlank()) {
                book.setAuthor(new Author(null, parts[2], 0));
            }
            book.setYear(Integer.parseInt(parts[3]));
            book.setPrice(new BigDecimal(parts[4]));   // <- баг 1: "не число" упадёт здесь
            book.setStatus(BookStatus.AVAILABLE);
            books.add(book);
        }
        return books;
    }

    /** "Чинит" имена авторов: делает первый символ заглавным. */
    private static void normalizeAuthors(List<Book> books) {
        for (Book book : books) {
            Author author = book.getAuthor();
            String name = author.getFullName();        // <- баг 2: author == null у книги 5
            book.getAuthor().setFullName(capitalize(name));
        }
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void printCatalog(List<Book> books) {
        for (int i = 0; i <= books.size(); i++) {      // <- баг 3: <= вместо <
            System.out.println(books.get(i));
        }
    }
}
