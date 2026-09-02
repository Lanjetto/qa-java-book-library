# book-library — демо «библиотека книг» (ветка `start`)

Учебный проект курса **«Java для QA»**. Ветка-контрольная точка `start` — консольное
приложение на чистой Java 21 (Gradle), без внешних зависимостей и тестов. Это фундамент
(Б4–Б7 Дня 1), от которого последовательно строятся все остальные ветки.

## 1. Что уже есть

- Домен «библиотека книг»: `Book`, `Author`, `BookStatus` (`AVAILABLE`, `RESERVED`, `SOLD`).
- Абстракция хранилища `BookStorage` + реализация в памяти `InMemoryBookStorage`
  (Map для id → O(1), List для порядка добавления, Set уникальных ISBN).
- `BookService` — бизнес-логика: `createBook` / `findById` / `findAll` / `search` /
  `findByStatus` / `topExpensive` / `countByStatus` / `totalPriceOf`.
- Учебные классы ООП для Дня 1: `Person` → `Reader`, `Librarian` (наследование, полиморфизм).
- Исключения: `BookNotFoundException`, `InvalidIsbnException`.
- `DebugDemo` — намеренно содержит 3 бага для отработки отладки в IntelliJ IDEA.
- Всё в пакете `library`.

## 2. Что добавлено в этой ветке

Это стартовая ветка: перечень из п.1 и есть «добавленное» относительно пустого проекта.

## 3. Как запустить / проверить

- `./gradlew run` — консольный прогон шагов Дня 1. В самом конце
  `DebugDemo.importCatalog()` падает на намеренном баге №1
  (`NumberFormatException`, «не число») — это учебная точка для отладки, а не ошибка сборки.
- `./gradlew build` — сборка. Тестов в `start` нет.

## 4. Задание «сделай сам» (День 1)

- Добавь книгу и найди её через `service.search(...)` по названию и по автору.
- Добавь в `Book` поле (например `int pages`) и посмотри, что сломается в
  `equals`/`hashCode`; поправь и перегенерируй.
- В `InMemoryBookStorage.save` добавь защиту от `null` ISBN.
- Через отладчик IntelliJ (Shift+F9) найди и исправь по одному 3 бага в `DebugDemo`.

## 5. Следующая ветка

`t1-junit5` — подключение JUnit 5 и первые юнит-тесты `BookServiceTest` / `BookStorageTest`.
