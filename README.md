# book-library — демо «библиотека книг» (ветка `t8-smoke-regression`)

Ветка-контрольная точка `t8` — про **стабильность прогона** (Б17), «не верить зелёному»:
smoke-проверка живости (`/actuator/health`), регресс-сценарий по всем статусам книги
и точечная параллель JUnit 5 там, где тесты изолированы.

## 1. Что уже есть (из `t7`)

- REST + JPA/Liquibase; Spring Security (basic auth) + RabbitMQ (`book.created`);
  Testcontainers-инфраструктура docker-тестов; REST Assured e2e, timezone-тест.

## 2. Что добавлено в `t8`

- **Spring Boot Actuator** (`spring-boot-starter-actuator`): `/actuator/health` — «точка живости».
  В `application.yml` включён web-эндпоинт `health` и `show-details: always` (для показа на занятии;
  в проде детали обычно скрывают — они раскрывают версии и состояние БД). В SecurityConfig
  (`!test`) `/actuator/health` уже был permitAll — smoke работает без авторизации.
- **Smoke-тест** `LibrarySmokeTest` (`@Tag("smoke")`, Б17): поднимает полный контекст и ждёт от
  `/actuator/health` код 200 и `status=UP`. Если приложение не стартует — smoke красный раньше,
  чем побегут долгие сценарии. Тег позволяет гонять быструю выборку отдельно:
  `./gradlew test --tests '*LibrarySmokeTest'`.
- **Регресс-сценарий** `BookLifecycleRegressionTest` (пакет `library.regression`): полный жизненный
  цикл статусов AVAILABLE → RESERVED → SOLD через REST, на каждом шаге перечитывает объект и
  сверяется. Пример изоляции (главное против флаков): книга создаётся с уникальным ISBN
  (`BookMother.unique()`), а листинг/поиск проверяются «по своей книге», а не «всего должно быть N»
  — такой тест не зависит от того, что до него сделали другие тесты/запуски.
- **Параллельный запуск JUnit 5** (Б17):
  - `junit-platform.properties`: `parallel.enabled=true`, но режим по умолчанию — `same_thread`
    (параллель выключена, пока её явно не попросят);
  - класс **без общего состояния** помечен `@Execution(CONCURRENT)` — `BookServiceTest`
    (юнит на Mockito: у каждого теста свои моки, БД нет) → его методы бегут в 3 потока
    (динамический пул: ядра × 0.75);
  - почему НЕ включаем параллель для всех классов сразу — см. javadoc `junit-platform.properties`
    (общая in-memory H2 и блокировка Liquibase при одновременном старте контекстов).
    Правило: **сначала изоляция, потом параллель**.

## 3. Как запустить / проверить

```
./gradlew build              # без Docker: юниты (параллельно) + H2-слайсы + smoke + регресс; docker-теги не трогает
./gradlew integrationTest    # с Docker: Postgres + RabbitMQ в контейнерах (10 тестов, из t6/t7)
```
Запуск руками (compose в WSL):
```
docker compose up -d postgres rabbitmq
./gradlew bootRun            # http://localhost:8080
curl -s http://localhost:8080/actuator/health                     # {"status":"UP", ...} — без авторизации
curl -s -u admin:secret http://localhost:8080/actuator/health     # то же, детали (show-details=always)
```
Повторяемость параллельного прогона:
```
./gradlew test               # прогнать 3 раза подряд — все зелёные (BookServiceTest идёт в 3 потока)
```
> **Где проверено:** `./gradlew build` локально (Windows, ✅; юниты бегут в 3 потока, smoke/регресс зелёные).
> `./gradlew integrationTest` — в WSL2 с Docker (✅, 10 тестов из t6/t7).

## 4. Задание «сделай сам» (Б17)

1. Добавь smoke на ещё один эндпоинт, например `/actuator/info`: включи `info` в
   `management.endpoints.web.exposure.include`, добавь блок `info` в `application.yml` и проверь в тесте.
2. **Регресс-задача:** напиши тест, который «завидует порядку», — например, в начале класса создаёт
   книгу со *фиксированным* ISBN (не `BookMother.unique()`) и ожидает в листинге ровно одну такую.
   Прогони класс дважды / вместе с соседними — увидишь флак от общего состояния. Почини изоляцией:
   уникальный ISBN и проверка «по своей книге».
3. Объясни, почему ретраи флаков — костыль, а не лечение (первопричина — общее состояние), и чем
   параллель усугубляет проблему.

## 5. Следующая ветка

`t9-allure-ci` — Allure-отчёты (плагин, `@Step`/`@Tag`/`@Description`, генерация отчёта) и CI:
`Jenkinsfile` + `ci.sh` — локальный симулятор пайплайна (Б18–Б19).
