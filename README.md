# book-library — демо «библиотека книг» (ветка `t1-junit5`)

Ветка-контрольная точка `t1-junit5` добавляет к `start` **JUnit 5** и первые юнит-тесты
(Б8–Б9 Дня 1). Пока без AssertJ, моков и покрытия — они появятся в `t2`.

## 1. Что уже есть (из `start`)

- Консольное приложение: домен `Book`/`Author`/`BookStatus`, `BookStorage` +
  `InMemoryBookStorage`, `BookService`, учебные классы `Person`→`Reader`/`Librarian`,
  исключения, `DebugDemo` (3 бага).

## 2. Что добавлено в `t1-junit5`

- `build.gradle`: `test { useJUnitPlatform() }`, зависимости JUnit 5 (BOM 5.14.x,
  `junit-jupiter`, `junit-platform-launcher`), лог тестов в консоль.
- `src/test/java/library/storage/BookStorageTest.java` — поведение коллекций хранилища
  (присвоение id, порядок `findAll`, `findById`, `deleteById`, уникальность ISBN).
- `src/test/java/library/service/BookServiceTest.java` — бизнес-логика сервиса
  (валидация ISBN, `findById`/`search`/`findByStatus`/`topExpensive`/`countByStatus`,
  исключения `BookNotFoundException`, `InvalidIsbnException`).
- Тесты написаны на чистом JUnit 5 (`org.junit.jupiter`), стиль `@BeforeEach` + `@Test`.

## 3. Как запустить / проверить

- `./gradlew test` — все тесты зелёные (16 шт.).
- `./gradlew build` — сборка с прогоном тестов.
- Отчёт: `build/reports/tests/test/index.html`.

## 4. Задание «сделай сам» (Б8–Б9)

- Добавь тест: поиск по автору должен находить книгу по подстроке без учёта регистра
  (сейчас в `BookServiceTest` один такой кейс — расширь).
- Добавь `@DisplayName` и негативный тест на `search(null)`.
- Перенеси фабрику книг из `seedCatalog()` в отдельный метод-помощник `book(...)`.
- Добавь тест на `totalPriceOf`.

## 5. Следующая ветка

`t2-lombok-assertj-coverage` — Lombok `@Data`, AssertJ, параметризованные тесты ISBN,
JaCoCo-покрытие.
