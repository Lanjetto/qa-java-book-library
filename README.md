# book-library — демо «библиотека книг» (ветка `t2-lombok-assertj-coverage`)

Ветка-контрольная точка `t2` поверх `t1`: **Lombok**, **AssertJ**, **параметризованные тесты**
и **JaCoCo-покрытие** (Б9 Дня 1).

## 1. Что уже есть (из `t1`)

- JUnit 5 (`useJUnitPlatform`), тесты `BookServiceTest` / `BookStorageTest`.

## 2. Что добавлено в `t2`

- **Lombok `@Data`/`@NoArgsConstructor`** на `Book` и `Author`: рукописные геттеры/сеттеры/
  `equals`/`hashCode`/`toString` удалены (рефакторинг без изменения поведения).
- **AssertJ** (`assertj-core`): тесты переведены с `assertEquals(...)` на fluent
  `assertThat(...).isEqualTo/containsExactly/extracting/containsEntry/singleElement/assertThatThrownBy`.
- **Параметризация** — `BookIsbnValidationTest`: `@ParameterizedTest` + `@MethodSource`
  (валидные 13/10-значные ISBN) и `@ValueSource`/`@NullAndEmptySource` (невалидные).
- **JaCoCo** (`id 'jacoco'`): `jacocoTestReport` (XML+HTML) и `jacocoTestCoverageVerification`
  с порогом LINE ≥ 0.85 для юнит-тестируемых пакетов `library.service.*` и `library.storage.*`
  (`App`/`DebugDemo`/ООП-демо классы в проверку не входят — это учебный код веток start/t1/t2).
  Проверка привязана к `check`.

## 3. Как запустить / проверить

- `./gradlew build` — тесты + отчёт JaCoCo + проверка порога.
- `./gradlew test` — только тесты.
- Отчёты: `build/reports/tests/test/index.html`, `build/reports/jacoco/test/html/index.html`.
- Фактическое покрытие строк: `BookService` 100%, `InMemoryBookStorage` 100% (по `jacocoTestReport`).

## 4. Задание «сделай сам» (Б9)

- Подними порог в `jacocoTestCoverageVerification` до 0.95 и добейся зелёного (добавь тесты
  на недопокрытые ветки `BookService.search`/`totalPriceOf`).
- Добавь параметризованный тест на `BookStatus`: `@EnumSource` прогоняет все значения.
- Добавь тест, который ломает `equals`/`hashCode` книги при одинаковом содержимом из разных
  `Author`-объектов, и разбери, почему (Lombok сравнивает поля по значению).

## 5. Следующая ветка

`t3-spring-rest` — Spring Boot Web: `BookApi` CRUD, DTO (`BookDto`/`CreateBookRequest`/
`UpdateBookRequest`/`ErrorResponseDto`), `@RestControllerAdvice`, in-memory хранилище.
