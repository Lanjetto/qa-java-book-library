package library.model;

/**
 * Сотрудник библиотеки.
 */
public class Librarian extends Person {

    private final String employeeCode;

    public Librarian(String fullName, int age, String employeeCode) {
        super(fullName, age);
        this.employeeCode = employeeCode;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    @Override
    public String roleDescription() {
        return "библиотекарь, табельный №" + employeeCode;
    }
}
