package library;

import library.debug.DebugDemo;
import library.exception.BookNotFoundException;
import library.model.Author;
import library.model.Book;
import library.model.BookStatus;
import library.model.Librarian;
import library.model.Person;
import library.model.Reader;
import library.service.BookService;
import library.storage.BookStorage;
import library.storage.InMemoryBookStorage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Точка входа. Пошагово демонстрирует темы части 1:
 * классы, наследование, абстракции, коллекции, лямбды, исключения, отладка.
 */
public class App {

    public static void main(String[] args) {
        BookStorage storage = new InMemoryBookStorage();
        BookService service = new BookService(storage);

        step1Classes(service);
        step2EqualsHashCode();
        step3Inheritance();
        step5Collections(storage);
        step6Lambdas(service);
        step7Exceptions(service);
        step8Debugging();
    }

    /** Шаг 1. Простые классы: создаём объекты Book и Author. */
    private static void step1Classes(BookService service) {
        Author martin = new Author(1L, "Роберт Мартин", 1952);
        Author goetz = new Author(2L, "Брайан Гоетц", 1965);

        Book cleanCode = new Book(null, "9780132350884", "Clean Code",
                martin, 2008, new BigDecimal("3000.00"), BookStatus.AVAILABLE);
        Book concurrency = new Book(null, "9780321349606", "Java Concurrency in Practice",
                goetz, 2006, new BigDecimal("4500.00"), BookStatus.AVAILABLE);
        Book effectiveJava = new Book(null, "9780134685991", "Effective Java",
                new Author(3L, "Джошуа Блох", 1959),
                2018, new BigDecimal("5200.00"), BookStatus.RESERVED);

        service.createBook(cleanCode);
        service.createBook(concurrency);
        service.createBook(effectiveJava);

        System.out.println("=== Шаг 1. Классы ===");
        System.out.println(martin);
        System.out.println(cleanCode);
    }

    /** Шаг 2. equals/hashCode/toString: одинаковые книги по содержимому равны. */
    private static void step2EqualsHashCode() {
        Book a = new Book(10L, "9780134685991", "Effective Java",
                new Author(3L, "Джошуа Блох", 1959),
                2018, new BigDecimal("5200.00"), BookStatus.AVAILABLE);
        Book b = new Book(10L, "9780134685991", "Effective Java",
                new Author(3L, "Джошуа Блох", 1959),
                2018, new BigDecimal("5200.00"), BookStatus.AVAILABLE);

        System.out.println("=== Шаг 2. equals/hashCode ===");
        System.out.println("a.equals(b) = " + a.equals(b));
        System.out.println("hashCode равны = " + (a.hashCode() == b.hashCode()));
        System.out.println("a == b (ссылки) = " + (a == b));
    }

    /** Шаг 3. Наследование и полиморфизм через абстрактный базовый класс Person. */
    private static void step3Inheritance() {
        Person reader = new Reader("Иван Петров", 30, 1001L);
        Person librarian = new Librarian("Анна Смирнова", 41, "LIB-77");

        System.out.println("=== Шаг 3. Наследование ===");
        printPerson(reader);
        printPerson(librarian);
    }

    private static void printPerson(Person person) {
        System.out.println(person.getFullName() + " — " + person.roleDescription());
    }

    /** Шаг 5. Коллекции: Map внутри хранилища + Set уникальных ISBN. */
    private static void step5Collections(BookStorage storage) {
        Set<String> isbns = ((InMemoryBookStorage) storage).getUniqueIsbns();

        System.out.println("=== Шаг 5. Коллекции ===");
        System.out.println("Книг в Map: " + storage.findAll().size());
        System.out.println("Уникальных ISBN в Set: " + isbns);
    }

    /** Шаг 6. Лямбды и Stream API: фильтрация, сортировка, группировка. */
    private static void step6Lambdas(BookService service) {
        System.out.println("=== Шаг 6. Лямбды и Stream API ===");

        List<Book> found = service.search("effective");
        System.out.println("search('effective'): " + found);

        List<Book> top2 = service.topExpensive(2);
        System.out.println("top-2 по цене: " + top2);

        Map<BookStatus, Long> byStatus = service.countByStatus();
        System.out.println("countByStatus: " + byStatus);

        BigDecimal total = service.totalPriceOf(BookStatus.AVAILABLE);
        System.out.println("Сумма AVAILABLE: " + total);
    }

    /** Шаг 7. Исключения: checked/unchecked, try/catch/finally. */
    private static void step7Exceptions(BookService service) {
        System.out.println("=== Шаг 7. Исключения ===");

        try {
            service.findById(999L);
        } catch (BookNotFoundException e) {
            System.out.println("Поймали BookNotFoundException: " + e.getMessage());
        } finally {
            System.out.println("finally выполняется всегда");
        }

        try {
            service.createBook(new Book(null, "не-isbn", "Bad",
                    new Author(99L, "X", 1980),
                    2020, new BigDecimal("1.00"), BookStatus.AVAILABLE));
        } catch (IllegalArgumentException e) {
            System.out.println("Поймали InvalidIsbnException как IllegalArgumentException: "
                    + e.getMessage());
        }
    }

    /** Шаг 8. Отладка: запускать через Debug (Shift+F9) с точками останова. */
    private static void step8Debugging() {
        System.out.println("=== Шаг 8. Отладка ===");
        DebugDemo.importCatalog();
    }
}
