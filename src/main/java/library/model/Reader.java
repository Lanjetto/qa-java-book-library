package library.model;

/**
 * Читатель библиотеки.
 */
public class Reader extends Person {

    private final Long readerCardId;

    public Reader(String fullName, int age, Long readerCardId) {
        super(fullName, age);
        this.readerCardId = readerCardId;
    }

    public Long getReaderCardId() {
        return readerCardId;
    }

    @Override
    public String roleDescription() {
        return "читатель, карта №" + readerCardId;
    }
}
