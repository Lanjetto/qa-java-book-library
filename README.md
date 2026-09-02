# book-library — демо «библиотека книг» (ветка `t4-jpa-postgres-liquibase`)

Ветка-контрольная точка `t4` переносит данные из памяти в **Postgres**: доступ — Spring Data JPA,
схема — **Liquibase**-миграциями, Hibernate схему не создаёт (`ddl-auto: validate`). Это блоки
**Б11** (Postgres в Docker + JPA + Liquibase) и **Б12** (Spring Test: `@SpringBootTest` / `@WebMvcTest` /
`@DataJpaTest`, профили) Дня 2. Учебные классы Дня 1 удалены.

## 1. Что уже есть (из `t3`)

- Spring Boot Web REST `BookApi` (`/api/books`): CRUD, поиск `?search=`, DTO (record), `@RestControllerAdvice`.
- Домен `Book`/`Author`/`BookStatus`; `BookService` c `updateBook/deleteById`; in-memory хранилище.

## 2. Что добавлено в `t4`

- **Spring Data JPA**: `library.repository.BookRepository extends JpaRepository<Book, Long>` +
  derived-методы `findByIsbn`, `findByTitleContainingIgnoreCase`, `findByStatus`, `findAllByOrderByIdAsc`.
- **JPA-сущности** (`library.model`):
  - `Book` — `@Entity @Table(name="books")`, `@Id @GeneratedValue(IDENTITY)`,
    `@Enumerated(EnumType.STRING) status`, `Instant createdAt` (**TIMESTAMPTZ**), проставляется в `@PrePersist`.
  - автор — `@Embeddable` `Author` (колонки `author_full_name`/`author_birth_year` в таблице `books`,
    без отдельной таблицы и **без id** автора — канон t4: книга владеет копией автора).
- **Liquibase**: `db/changelog/db.changelog-master.yaml`, changeset `1-create-books`; при старте создаётся
  `books`, выполненное помечается в `DATABASECHANGELOG` (повторный старт не дублирует).
  `Hibernate ddl-auto: validate` — только сверяет сущности со схемой.
- **Конфигурация**: `application.yml` — профиль по умолчанию Postgres (`localhost:5434/library`);
  `docker-compose.yml` — `postgres:16` (порт 5434 наружу).
- **Тесты** (профиль `test` → **H2** `MODE=PostgreSQL`, схема — те же Liquibase-миграции):
  - `BookRepositoryTest` — `@DataJpaTest`: срез JPA-слоя, каждый тест в транзакции с откатом;
  - `BookApiContextTest` — `@SpringBootTest(RANDOM_PORT)`: полный контекст + HTTP-CRUD по TestRestTemplate
    (это и есть «локальный bootRun» без Docker и источник покрытия `BookService`);
  - `BookApiSliceTest` — `@WebMvcTest(BookApi.class)`: web-слой с моком сервиса (`@MockitoBean`).
- **`BookDto`** дополнен `createdAt` (в JSON сериализуется в UTC с «Z»). `Author`-значение: REST-контракт
  принимает автора строкой (полное имя) → `new Author(name, 0)`.
- **Удалены** (переезд на БД): учебные `Person`/`Reader`/`Librarian`/`App`/`DebugDemo` и
  `BookStorage`/`InMemoryBookStorage`; `BookService` ужат до REST-операций (хранилище заменено репозиторием).

### Замечания по схеме и домену

- Колонка года названа **`publication_year`**, а не `year`: `YEAR` — зарезервированное слово в H2
  (и ряде СУБД). На Postgres допустимо и `year`, но переносимый вариант — `publication_year`.
- Год рождения автора через REST не передаётся (автор — строка), поэтому в БД у книг, созданных через API,
  `author_birth_year = 0`; вручную его можно задать в `psql`/тесте (`@DataJpaTest`).
- Поиск `?search=` остался фильтром по списку на уровне Java (см. комментарий в `BookService.search`).

## 3. Как запустить / проверить

**Локально, без Docker (проверено на Windows):** тесты сами поднимают контекст + Liquibase на H2.
```
./gradlew build      # 15 тестов + JaCoCo (порог ≥0.85 на library.service.*)
```
**Против настоящего Postgres** (Liquibase применяется к реальной БД):
```
docker compose up -d postgres        # на машине преподавателя — из WSL2
./gradlew bootRun                    # http://localhost:8080
```
Smoke (ASCII-тело; кириллицу слать файлом в UTF-8 — иначе curl в Windows bash ломает кодировку):
```
curl -s -X POST http://localhost:8080/api/books -H "Content-Type: application/json" \
     -d '{"isbn":"9780132350884","title":"Clean Code","author":"Martin","year":2008,"price":3000.00,"status":"AVAILABLE"}'
curl -s http://localhost:8080/api/books
```
Проверить, что схема реально легла в БД (из WSL/psql):
```sql
SELECT id, isbn, title, author_full_name, author_birth_year, created_at FROM books;
SELECT id, author, filename FROM DATABASECHANGELOG;   -- содержит 1-create-books
```
> **Где проверено:** сборка и тесты — локально (Windows, `./gradlew build` ✅). Liquibase-миграции и CRUD
> **против настоящего Postgres** — проверено в этой сессии на локальной БД (Windows-служба PostgreSQL,
> та же `db.changelog-master.yaml`; канонный вариант для слушателей — `postgres:16` из `docker-compose.yml`).

## 4. Задание «сделай сам» (Б11–Б12)

1. Добавь Liquibase-миграцию (новый changeset `2-add-pages`): колонка `pages INT` в `books`; перезапусти
   приложение и убедись, что миграция применилась один раз, а повторный старт её не дублирует.
2. Добавь в `BookRepository` производный метод (например, `List<Book> findByYear(int year)`) и напиши
   `@DataJpaTest` на него (по образцу `BookRepositoryTest`).
3. Сравни три вида тестов на одном домене: `@DataJpaTest` / `@WebMvcTest` / `@SpringBootTest` —
   что каждый поднимает и почему `@DataJpaTest` откатывает данные, а HTTP-тест (`BookApiContextTest`) — нет.

## 5. Следующая ветка

`t5-restassured-mockito-testdata` — тестовые данные (`BookMother`/Instancio), Mockito-юниты сервиса,
интеграционный `BookCrudTest` на **REST Assured**, тест timezone/Instant (Б13–Б14).
