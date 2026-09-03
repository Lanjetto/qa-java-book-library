# book-library — демо «библиотека книг» (ветка `t9-allure-ci`)

Ветка-контрольная точка `t9` про то, как сделать прогон **объяснимым** и как автотесты
живут в **CI** (Б18–Б19): Allure-отчёт поверх JUnit 5 и пайплайн «как код».

## 1. Что уже есть (из `t8`)

- REST + JPA/Liquibase; Security + RabbitMQ; Testcontainers; Actuator (`/actuator/health`),
  smoke-тест, регресс-сценарий по статусам, точечная параллель JUnit 5.

## 2. Что добавлено в `t9`

- **Allure** (Б19), три части: адаптер `io.qameta.allure:allure-junit5:2.27.0` пишет JSON на каждый
  тест в `build/allure-results/`; плагин `io.qameta.allure` (в `plugins {}`) генератор собирает из них
  статический HTML (`./gradlew allureReport`); метаданные отчёта лежат в `src/test/resources/allure/`:
  `environment.properties` (вкладка Overview) и `categories.json` (Product/Test defects).
- **Аннотации в тестах:**
  - `@Epic/@Feature/@Story` — иерархия для вкладки **Behaviors** (`BookApiContextTest` и
    `BookLifecycleRegressionTest`: Epic «Книги», Feature «REST API…» / «Жизненный цикл статусов»);
  - `@Step("… {id}")` — HTTP-вызовы регресс-сценария (`createBook`/`book`/`patchStatus`) видны
    ступенями в дереве теста (weaving через `aspectjweaver=true` в `allure {}`);
  - `@Description`, `@Severity` — контекст и приоритет теста.
- **CI (Б18):**
  - `Jenkinsfile` — declarative pipeline: `stage('Сборка и тесты')` → `stage('Allure-отчёт')`,
    артефакты и JUnit-XML архивируются в `post { always { … } }` (сохраняются и при падении);
    интеграционные docker-тесты закомментированы (им нужен Docker-агент);
  - `ci.sh` — локальный симулятор тех же шагов без Jenkins (`./gradlew test` + `./gradlew allureReport`).
- `build.gradle`: конфигурация `allure { version = '2.27.0'; autoconfigure = true; aspectjweaver = true }`;
  после генерации отчёт поднимается на уровень `build/reports/allure-report/`, чтобы `index.html` лежал
  по ожидаемому пути (плагин кладёт его в подпапку). Встроенный `copyCategories` отключён —
  категории кладём сами (на повторном прогоне его маркер-файл ронял `allureReport`).

> Версии: плагин Allure 3.2.0 (под Gradle 9.7), раннер/адаптер Allure 2.27.0. Allure — слой отчётности
> **над** JUnit 5, а не замена раннера: падение по-прежнему фейлит сборку, Allure делает его объяснимым.

## 3. Как запустить / проверить

```
./gradlew build                  # без Docker: юниты + H2-слайсы + smoke/регресс (docker-теги не трогает)
./gradlew test allureReport      # сгенерировать отчёт
# открыть: build/reports/allure-report/index.html  (вкладки Behaviors, Suites, Timeline, Categories)
./gradlew allureServe            # или поднять локальный сервер отчёта
./ci.sh                          # локальный симулятор CI-пайплайна (те же шаги, что в Jenkinsfile)
./gradlew integrationTest        # с Docker: Postgres + RabbitMQ (10 тестов из t6/t7)
```
> **Где проверено:** локально (Windows, ✅): `./gradlew build`, `./gradlew test allureReport` и `./ci.sh`
> (дважды подряд — повторяемость). В отчёте видны ступени `@Step`, теги/`@Epic` (Behaviors),
> `environment.properties`/`categories.json`. `Jenkinsfile` — линт/ревью (живого Jenkins нет).

## 4. Задание «сделай сам» (Б19)

1. Разметь `@Step` свой e2e-сценарий (например, `BookCrudTest`) и открой отчёт — увидишь дерево шагов.
2. Добавь в `Jenkinsfile` этап `Publish Allure Report` (например, через плагин `allure-jenkins-plugin`)
   или шаг архивации ещё одного артефакта.
3. Объясни, почему Allure-результаты (`allure-results`) архивируют отдельно от HTML-отчёта (тренды,
   пересборка отчёта из нескольких прогонов) и почему `post { always }`, а не `onSuccess`.

## 5. Следующая ветка

`t10-load-jfr` — нагрузка Gatling (сценарий на `/api/books`) и профилирование JVM:
JFR-запись, async-profiler, JMH-бенчмарк, virtual threads (Б20–Б21).
