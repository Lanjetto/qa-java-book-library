# book-library — демо «библиотека книг» (ветка `t5-restassured-mockito-testdata`)

Ветка-контрольная точка `t5` добавляет **тестовые данные и моки** (Б13–Б14): главный API-инструмент
курса — **REST Assured**, фабрику данных `BookMother`, генератор `Instancio`, Mockito-юниты сервиса
и разбор темы timezone/`Instant`. **main-код не меняется** — только тестовая инфраструктура.

## 1. Что уже есть (из `t4`)

- Spring Boot REST `BookApi` (`/api/books`) на Spring Data JPA + Liquibase (Postgres/H2-профиль).
- Тесты Spring Test: `BookRepositoryTest` (`@DataJpaTest`), `BookApiSliceTest` (`@WebMvcTest`),
  `BookApiContextTest` (`@SpringBootTest` + TestRestTemplate, H2).

## 2. Что добавлено в `t5`

- **Зависимости (test):** `rest-assured 5.5.7`, `mockito-junit-jupiter 5.23.0`, `instancio-junit 5.6.0`.
- **`build.gradle`:** интеграционные тесты с `@Tag("docker")` **исключены из дефолтного `test`** и вынесены
  в отдельную таску `integrationTest` (им нужна настоящая Postgres-БД).
- **Тестовые данные (Б14):**
  - `src/test/java/library/testdata/BookMother.java` — фабрика Object Mother: готовые `CreateBookRequest`
    (`cleanCode()`, `effectiveJava()`, `unique()` с уникальным ISBN), доменные `Book`, `uniqueIsbn()`.
  - генерация случайных объектов `Instancio` — пример использования в `BookServiceTest`
    (книга с «правильным» ISBN через `set(field(...))`).
- **REST Assured (Б13):**
  - `src/test/java/library/api/BookApiSpecs.java` — `RequestSpecification`/`ResponseSpecification`
    (убирают дублирование contentType/статуса);
  - `src/test/java/library/api/BookCrudTest.java` — интеграционный CRUD `@Tag("docker")` против Postgres:
    создать 201 → прочитать → PATCH → удалить 204 → 404; негативные 404/400; список.
- **Mockito-юнит (Б14.4):** `src/test/java/library/service/BookServiceTest.java` —
  `@ExtendWith(MockitoExtension.class)`, `@Mock BookRepository`, `@InjectMocks BookService`;
  покрывает ветки, не достижимые сквозным HTTP-тестом (невалидный ISBN, пустой поиск, отсутствующая книга).
- **Timezone/Instant (Б14.2):** `src/test/java/library/api/BookDateConsistencyTest.java` — `@Tag("docker")`,
  сверяет `createdAt` из JSON API и из БД на настоящем Postgres (`TIMESTAMPTZ` = момент в UTC).

### Замечания (сверено с реальным контрактом t4)

- Автор в REST-контракте — **строка** (полное имя), а не объект; `status` обязателен (в `BookMother`
  всегда AVAILABLE, при необходимости меняется в тесте). Это расхождение текста day2/SCRIPT.md
  с фактическим кодом зафиксировано в HANDOFF (§6.1) и правится в Фазе 4.
- Негативный сценарий «дубликат ISBN → 409» НЕ добавлен: продукт не отдаёт 409 (нет обработки
  конфликта уникальности). В t5 негативные сценарии — 404 и 400 от bean-валидации.

## 3. Как запустить / проверить

**Юниты и H2-слайсы — локально, без Docker:**
```
./gradlew build        # 34 теста + JaCoCo (порог ≥0.85 на library.service.*)
```
**Интеграционные (`@Tag("docker")`) против настоящего Postgres:**

Вариант для слушателей — Postgres из `docker-compose.yml` (поднимается в WSL):
```bash
docker compose up -d postgres
wsl -e bash -lc "cd ~/book-library && ./gradlew integrationTest"
```
Локально без Docker — против Windows-службы PostgreSQL (в этой сессии проверено на PG17, БД `library_t5`):
```bash
psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE library_t5;"
SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/library_t5' \
SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=root \
  ./gradlew integrationTest
psql -U postgres -h localhost -p 5432 -c "DROP DATABASE library_t5;"
```
> **Где проверено:** `./gradlew build` локально (Windows, ✅); `integrationTest` (BookCrudTest +
> BookDateConsistencyTest) — против настоящего Postgres 17 на `localhost:5432` (✅, 5 тестов).
> Контракт `TIMESTAMPTZ`/`Instant`/JSON-UTC подтверждён на реальной БД, а не только на H2.

## 4. Задание «сделай сам» (Б13–Б14)

1. Расширь `BookMother`: книга в статусе «в резерве» (`RESERVED`); добавь негативный сценарий REST Assured
   (404/400) в `BookCrudTest`.
2. Перепиши один «старый» тест с ручным `init()` на `@BeforeEach` + фабрику `BookMother` (Б14.5:
   маленькими шагами, зелёный прогон до и после).
3. Попробуй REST Assured JSON Schema: подключи модуль `json-schema-validator`, схему
   `src/test/resources/schemas/book.json` — по образцу из `QA\v2\10-rest-api.md`.

## 5. Следующая ветка

`t6-testcontainers-e2e` — внешний Postgres в интеграционных тестах заменяется **Testcontainers**-контейнером
(тест сам поднимает БД и накатывает Liquibase); `BookCrudTest` становится e2e (Б15).
