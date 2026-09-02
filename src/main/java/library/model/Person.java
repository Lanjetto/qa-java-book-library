package library.model;

/**
 * Абстрактный базовый класс человека, работающего с библиотекой.
 */
public abstract class Person {

    private String fullName;
    private int age;

    protected Person(String fullName, int age) {
        this.fullName = fullName;
        this.age = age;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Абстрактный метод: каждый подкласс отвечает, что он делает в библиотеке.
     */
    public abstract String roleDescription();
}
