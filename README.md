# book-library — демо «библиотека книг» (ветка `t3-spring-rest`)

Ветка-контрольная точка `t3` превращает консольное приложение в **Spring Boot Web REST-сервис**
(Б10 Дня 2). Хранилище пока in-memory; Postgres/JPA появляются в `t4`.

## 1. Что уже есть (из `t2`)

- Домен `Book`/`Author`/`BookStatus` на Lombok, `BookService` + `InMemoryBookStorage`,
  JUnit 5/AssertJ/JaCoCo unit-тесты, параметризация ISBN.

## 2. Что добавлено в `t3`

- **Spring Boot 3.5** (web + validation): `LibraryApplication`, `application.yml`.
- **DI**: `@Service BookService`, `@Repository InMemoryBookStorage`, контроллер получает сервис
  через конструктор; `BookService` дополнен `updateBook(id, UpdateBookRequest)` и `deleteById(id)`.
- **REST `BookApi`** (`/api/books`):
  - `GET /api/books[?search=]` — список (или поиск по подстроке);
  - `GET /api/books/{id}`; `POST /api/books` (201 + Location); `PATCH /api/books/{id}`;
    `DELETE /api/books/{id}` (204).
- **DTO (record)**: `BookDto` (наружу автор — строка), `CreateBookRequest` (+bean-валидация),
  `UpdateBookRequest` (PATCH: только не-null поля), `ErrorResponseDto`.
- **`ApiExceptionHandler`** (`@RestControllerAdvice`): 404 для `BookNotFoundException`,
  400 для `InvalidIsbnException`/ошибок валидации/нераспарсенного JSON — единый формат `ErrorResponseDto`.
- **Исправлен баг** `InMemoryBookStorage.save`: повторный `save` того же id (PATCH) больше
  не дублирует книгу в `List` — замена происходит на месте.
- Console `App`/`DebugDemo`/ООП-демо классы остались в исходниках (история Дня 1), но точка входа —
  `LibraryApplication`.

## 3. Как запустить / проверить

- `./gradlew bootRun` — приложение на `http://localhost:8080`.
- Smoke (ASCII-тело; кириллицу слать файлом в UTF-8):
  ```
  curl -s http://localhost:8080/api/books
  curl -s -X POST http://localhost:8080/api/books -H "Content-Type: application/json" \
       -d '{"isbn":"9780132350884","title":"Clean Code","author":"Martin","year":2008,"price":3000.00,"status":"AVAILABLE"}'
  curl -s -X PATCH http://localhost:8080/api/books/1 -H "Content-Type: application/json" -d '{"status":"SOLD"}'
  curl -s -X DELETE http://localhost:8080/api/books/1 -o /dev/null -w "%{http_code}\n"   # 204
  curl -s http://localhost:8080/api/books/999   # 404 + ErrorResponseDto
  ```
- `./gradlew build` — unit-тесты + JaCoCo (порог ≥0.85 для service/storage; фактически 100%).

## 4. Задание «сделай сам» (Б10)

- Добавь эндпоинт `GET /api/books/search?q=...` (или переиспользуй `?search=`) и тест на поиск.
- Добавь в `CreateBookRequest` поле `birthYear` автора и прокини в домен.
- Напиши `@WebMvcTest(BookApi.class)` c `@MockBean BookService` на один из эндпоинтов.
- Разбери, почему `BookService` теперь `@Service`, а хранилище `@Repository`, и что даёт внедрение
  через интерфейс `BookStorage`.

## 5. Следующая ветка

`t4-jpa-postgres-liquibase` — Spring Data JPA `BookRepository`, миграции Liquibase,
Postgres в Docker (`docker-compose.yml`), `@Embeddable`-маппинг автора.
