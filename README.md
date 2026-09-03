# book-library — демо «библиотека книг» (ветка `t6-testcontainers-e2e`)

Ветка-контрольная точка `t6` переводит интеграционные тесты на **Testcontainers** (Б15):
тест **сам поднимает настоящий Postgres-контейнер** и накатывает Liquibase — внешний Postgres
из docker-compose для `integrationTest` больше не нужен. `BookCrudTest` становится самодостаточным e2e.

## 1. Что уже есть (из `t5`)

- REST Assured `BookCrudTest`/`BookApiSpecs`, Mockito-юниты `BookServiceTest`, `BookMother`/Instancio,
  timezone-тест `BookDateConsistencyTest` — все `@Tag("docker")`-интеграции против внешнего Postgres.
- Таска `integrationTest` (docker-теги исключены из дефолтного `test`).

## 2. Что добавлено в `t6`

- **Зависимости (test):** `spring-boot-testcontainers` (даёт `@ServiceConnection`), `testcontainers:postgresql`
  и `testcontainers:junit-jupiter` 1.21.4.
- **`AbstractPostgresIT`** (`library.testcontainers`) — один Postgres-контейнер `postgres:16-alpine` на весь
  docker-прогон: `@ServiceConnection` настраивает `DataSource` под контейнер (без ручного
  `@DynamicPropertySource`), Liquibase применяется в контейнер.
  - ⚠️ Контейнер стартуется вручную (`static { }`), а не `@Testcontainers`/`@Container`: расширение
    останавливает static-контейнер **после каждого тест-класса**, а Spring кэширует application-context
    на остановленную БД → следующий класс падает. Один живой контейнер на JVM — надёжнее.
- **`BookCrudTest`** переведён с внешнего Postgres (порт 5434) на контейнер — e2e больше не требует
  `docker compose up` заранее.
- **`BookDateConsistencyTest`** — timezone/Instant-проверка тоже на контейнерной БД.
- **`BookRepositoryPostgresTest`** — `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)`
  (используем контейнер вместо H2): `findByStatus`/`findByTitleContainingIgnoreCase`/`findByIsbn`
  на настоящем Postgres (диалект не эмулируется).

## 3. Как запустить / проверить

Нужен **Docker**. Интеграционные (`@Tag("docker")`) исключены из дефолтного прогона:
```
./gradlew build              # без Docker: юниты + H2-слайсы, docker-теги не трогает
./gradlew integrationTest    # с Docker: Testcontainers сам поднимает Postgres (8 тестов)
```
В этой сессии `integrationTest` прогнан **внутри WSL2** (там доступен docker-сокет):
```
wsl -e bash -lc "cd ~/it/book-library && ./gradlew integrationTest"
```
> **Где проверено:** `./gradlew build` локально (Windows, ✅, docker-теги не входят); `./gradlew
> integrationTest` — в WSL2 с Docker (✅ 8 тестов: BookCrudTest e2e-CRUD 4, BookDateConsistencyTest 1,
> BookRepositoryPostgresTest 3). БД поднимал сам тест, внешний Postgres не требовался.

## 4. Задание «сделай сам» (Б15)

1. Добавь в e2e `BookCrudTest` сценарий поиска без учёта регистра (`GET /api/books?search=`) на настоящем
   Postgres и негативный e2e: удаление несуществующей книги → 404.
2. Попробуй `.withReuse(true)` (при `TESTCONTAINERS_REUSE_ENABLE=true`) и объясни, почему на CI reuse
   выключен (БД хранит состояние между прогонами).
3. Сравни «H2-гибрид» (быстрые slice-тесты в IDE) с интеграционными на Testcontainers — что покрывает каждый.

## 5. Следующая ветка

`t7-security-rabbitmq` — Spring Security (basic auth) + RabbitMQ-эмиттер `book.created`; тесты с
авторизацией (401/403) и публикацией события (Б16).
