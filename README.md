# book-library — демо-проект курса «Java для QA» (ветка `final`)

Учебный проект «библиотека книг», который **растёт вместе с курсом**: от консольной программы на Java
(ветка `start`) через REST + БД + тесты до стабильности прогона, Allure-отчётов, CI и нагрузки/JVM.
Каждая ветка — контрольная точка: **строго последовательно** `start → t1 → t2 → … → final`,
от предыдущей ветки к следующей добавляется ровно одна тема.

> Это «обзорная» ветка: код здесь = сумма всех t1…t10, а README отвечает на вопрос
> «что смотреть на каждом шаге курса». GitHub-дефолт репозитория — ветка `start` (День 1, Б4).

---

## 1. Что это и как устроено

**Домен:** книга (`Book`) с автором (`Author`, встроенное значение), статусом AVAILABLE/RESERVED/SOLD.
История: `start…t3` — in-memory и «толстый» учебный сервис; `t4` — Spring Data JPA + Liquibase (Postgres),
`Book` становится JPA-сущностью, появляется `createdAt` (UTC).

**Стек (версии проверены на машине преподавателя):** Java 21, Gradle wrapper 9.7, Spring Boot 3.5.14,
JUnit 5.14 + AssertJ 3.27 + Mockito 5.23, Spring Security, RabbitMQ, Testcontainers 1.21, REST Assured 5.5,
Instancio 5.6, Liquibase, JaCoCo, Allure 2.27, Gatling 3.15, JMH 1.37.

**Структура типов тестов по мере роста проекта:**
unit (`BookServiceTest`, Mockito) → web-слайс (`@WebMvcTest`) → JPA-слайс (`@DataJpaTest`) →
полный контекст (`@SpringBootTest` + TestRestTemplate) → интеграционные REST Assured против Postgres →
Testcontainers e2e (Postgres+RabbitMQ) → smoke/регресс → отчёт Allure → нагрузка Gatling.

## 2. Как поднять приложение

```bash
# Быстрый старт без внешних сервисов — тесты идут на H2 (профиль test), см. ветки t4+:
./gradlew build                 # юниты + H2-слайсы + smoke/регресс; docker-теги не трогает

# Полный стенд: Postgres + RabbitMQ (docker-compose, порт 5434/5672) + приложение:
docker compose up -d postgres rabbitmq
./gradlew bootRun               # http://localhost:8080  (API под basic auth: admin/secret)
curl -s http://localhost:8080/actuator/health                    # {"status":"UP"}
curl -s -u admin:secret http://localhost:8080/api/books          # [] (пусто, пока не создали книг)
curl -s -u admin:secret -H 'Content-Type: application/json' \
  -d '{"isbn":"9780132350884","title":"Чистый код","author":"Роберт Мартин","year":2008,"price":899.99,"status":"AVAILABLE"}' \
  http://localhost:8080/api/books                                # 201 Created
```
Миграции схемы — Liquibase (`src/main/resources/db/changelog/`), применяются при старте контекста.

## 3. Карта веток: ветка → что демонстрирует → как проверить

| Ветка | Демонстрирует | Как проверить (коротко) | Что смотреть |
|---|---|---|---|
| `start` | Java-фундамент: классы, enum, `BookStorage`/`BookService`/`App`, DebugDemo, отладка в IntelliJ (Б4–Б7) | `./gradlew build`; `App.main()` печатает Шаги 1–7 и падает на Шаге 8 (для Б7) | `DebugDemo`, «толстый» сервис Дня 1 |
| `t1-junit5` | Первые тесты: `useJUnitPlatform`, `BookServiceTest`/`BookStorageTest` (Б8) | `./gradlew test` (16 тестов) | src/test, структура |
| `t2-lombok-assertj-coverage` | Lombok `@Data`, AssertJ, параметризация ISBN, JaCoCo-порог (Б9) | `./gradlew build` (JaCoCo на service) | jacocoTestReport |
| `t3-spring-rest` | Spring Web: REST CRUD, DTO, `@RestControllerAdvice`, in-memory (Б10) | `bootRun` + curl (см. §2) | `BookApi`, ErrorResponseDto |
| `t4-jpa-postgres-liquibase` | Spring Data JPA, Liquibase-миграции, `@SpringBootTest`/слайсы, профили (Б11–Б12) | `./gradlew build` на H2; против Postgres — compose | `BookRepository`, changelog, `createdAt` UTC |
| `t5-restassured-mockito-testdata` | REST Assured, `BookMother`/Instancio, Mockito-юниты, timezone-тест (Б13–Б14) | `./gradlew build`; `integrationTest` с Postgres | `BookCrudTest`, `BookServiceTest` |
| `t6-testcontainers-e2e` | Testcontainers Postgres, e2e CRUD, `@ServiceConnection` (Б15) | `./gradlew integrationTest` (WSL+Docker) | `AbstractPostgresIT` |
| `t7-security-rabbitmq` | Spring Security basic auth, событие `book.created` в RabbitMQ (Б16) | `./gradlew integrationTest` (Postgres+Rabbit) | `SecurityConfig`, `messaging/`, 401-тест |
| `t8-smoke-regression` | Actuator `/actuator/health` + smoke, регресс по статусам, параллель JUnit5 (Б17) | `./gradlew build`; `curl …/actuator/health` | `LibrarySmokeTest`, `BookLifecycleRegressionTest`, junit-platform.properties |
| `t9-allure-ci` | Allure-отчёт (`@Step`/`@Epic`/…), CI `Jenkinsfile`+`ci.sh` (Б18–Б19) | `./gradlew test allureReport`; `./ci.sh` | `allure-report/index.html`, Behaviors |
| `t10-load-jfr` | Gatling-сценарий, JFR-запись тестов, JMH-бенчмарк, virtual threads (Б20–Б21) | `./gradlew jmhRun`; `./gradlew test -Pjfr`; `gatlingRun` с поднятым сервером | `BookSearchSimulation`, `jmh/`, `jvm/` |
| `final` | Всё вместе: код = t1…t10, обзорная README, контроль чистоты (Б22) | `git ls-files` без `build/`/`.gradle/` | эта таблица + §4 |

## 4. Что смотреть на каждом шаге курса (быстрая навигация преподавателя)

- **Б4–Б7 (День 1)** — ветка `start`: синтаксис/классы/enum, коллекции и лямбды, equals/hashCode/Lombok,
  отладка. Квиз A после Б7.
- **Б8–Б9 (День 1)** — `t1` → `t2`: как запускать тесты, JUnit5+AssertJ+параметризация+JaCoCo. Квиз B.
- **Б10–Б12 (День 2)** — `t3` → `t4`: REST, Postgres в Docker + JPA + Liquibase, «вес» теста и профили.
- **Б13–Б16 (День 2)** — `t5` → `t7`: REST Assured и тестовые данные; Testcontainers/e2e; Security+RabbitMQ.
  Квиз C после Б14, Квиз D после Б17.
- **Б17–Б19 (День 2)** — `t8` → `t9`: не верить зелёному (smoke/флейки/параллель); Allure; CI как код.
  Квиз E.
- **Б20–Б21 (День 2)** — `t10`: нагрузка Gatling и JVM (JFR/JMH/virtual threads).
- **Б22 (День 2)** — `final`: эта карта веток + разбор матрицы quiz-app. Квиз F.

## 5. Контроль чистоты репозитория

```bash
git ls-files | grep -E 'build/|\.gradle/|allure-results/|ajcore|\.jfr$'   # должно быть пусто
git ls-files | wc -l        # только исходники/конфиги/README (без артефактов сборки)
./gradlew build -x integrationTest   # финальный контроль: юниты + H2 + smoke + регресс зелёные
```

## 6. Как двигаться по веткам (для слушателей)

```bash
git clone https://github.com/Lanjetto/qa-java-book-library.git
git checkout <branch>          # start → t1-junit5 → t2-… → final
./gradlew clean build          # каждая ветка собирается сама по себе
```
Смысл контрольных точек: на каждом шаге видно **ровно одно** изменение против предыдущей ветки
(`git diff t1..t2` и т.п.), а в README каждой ветки — «задание сделай сам».
