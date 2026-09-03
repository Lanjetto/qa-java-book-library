# book-library — демо «библиотека книг» (ветка `t10-load-jfr`)

Ветка-контрольная точка `t10` — язык «куда девается время» (Б20–Б21): нагрузочный сценарий Gatling
и инструменты JVM-профилирования (JFR, JMH, virtual threads). main/test-логика почти не меняется —
добавляются инструменты и один демо-тест.

## 1. Что уже есть (из `t9`)

- REST + JPA/Liquibase; Security + RabbitMQ; Testcontainers; Actuator + smoke; Allure-отчёт + CI
  (`Jenkinsfile`/`ci.sh`).

## 2. Что добавлено в `t10`

- **Gatling** (Б20), плагин `io.gatling.gradle` (3.15.1.3, Java-сценарии без Scala):
  - `src/gatling/java/BookSearchSimulation.java` — «минимальный честный» сценарий: `GET /api/books`
    и поиск `?search=`. Правила честности зашиты в код: `check(status().is(200))`, фидер
    `csv("data/search-queries.csv").circular()` (разные запросы, не одна ветка), think time `pause`,
    в `setUp` assertions на **p99** и % успеха (нарушение → красная сборка).
  - фидер `src/gatling/resources/data/search-queries.csv`.
- **JFR** (Б21): запись тестовой JVM одной командой:
  `./gradlew test -Pjfr` → `build/jfr/test.jfr` (`-XX:StartFlightRecording=settings=profile,dumponexit`,
  оверхед 1–2 %; `.jfr` открывается в IntelliJ — CPU/GC/Lock/Socket). Живому процессу — `jcmd <pid> JFR.start …`.
- **JMH** (Б21): `src/test/java/library/jmh/BookServiceSearchBenchmark.java` — микро-бенчмарк
  `BookService.search` (500 книг, варианты «совпадение в конце» / «без совпадений»). Раннер — без плагина,
  таска `jmhRun` (JavaExec на `org.openjdk.jmh.Main`): warmup/forks/короткие итерации — «бенчмарк жив»;
  честные цифры снимают дольше. Внимание: JMH ≠ нагрузочный тест (Б21).
- **Virtual threads** (Б21): `src/test/java/library/jvm/VirtualThreadsTest.java` — 50 блокирующих задач
  (по 100 мс) на `newVirtualThreadPerTaskExecutor()` перекрываются: факт «прошло ~время одной, а не 50 ×»;
  класс изолирован и помечен `@Execution(CONCURRENT)`.
- `build.gradle`: JMH-зависимости (`jmh-core` + annotation processor), таска `jmhRun`, флаг `-Pjfr`.

## 3. Как запустить / проверить

```
./gradlew build            # тесты (37) без Docker; gatling/jmh в check НЕ входят
./gradlew test -Pjfr       # + JFR-запись тестовой JVM: build/jfr/test.jfr
./gradlew jmhRun           # короткий JMH-прогон BookService.search (~минута)
```

Нагрузка Gatling (нужен запущенный сервер):
```
# терминал 1 — поднять приложение (любой способ):
docker compose up -d postgres rabbitmq && ./gradlew bootRun     # WSL+compose
# или локально без Docker — H2 (профиль test не в jar; см. примечание ниже)
# терминал 2:
./gradlew gatlingRun                # по умолчанию http://localhost:8080
./gradlew gatlingRun -DbaseUrl=http://localhost:8080   # если URL другой
# отчёт: build/reports/gatling/…/index.html (Response Time Distribution, перцентили, RPS)
```
> **Где проверено:** `./gradlew build` ✅ локально (Windows, 37 тестов); JMH `jmhRun` ✅ (оба варианта,
> ~80 µs/op на 500 книг); JFR `-Pjfr` ✅ (`build/jfr/test.jfr`, ~9 МБ). Gatling: **компиляция сценария** ✅
> (`compileGatlingJava`); полный `gatlingRun` требует запущенного приложения — прогнать на занятии
> (как docker-теги t6/t7: локально без сервера честный прогон не сделать). async-profiler — тяжёлый
> полноценный прогон на Linux/WSL по желанию (Б21).

## 4. Задание «сделай сам» (Б20–Б21)

1. Добавь assertion на **p95** в `BookSearchSimulation` и второй сценарий (поиск по автору); сравни отчёты
   двух прогонов при одинаковом окружении (только так сравнение корректно).
2. JMH: добавь бенчмарк на `updateBook`/`findById` и объясни, почему цифры микро-бенча нельзя переносить
   на «весь сервис».
3. Сними JFR на медленном тесте (например, том, что много аллоцирует): найди, где время — в генерации
   данных/GC, и «вылечи» (переиспользование фикстур, меньше мусора). Метод «до/после».

## 5. Следующая ветка

`final` — всё вместе: финальный README (как поднять, карта всех веток «ветка → что демонстрирует»,
что смотреть на каждом шаге курса) + контроль чистоты репозитория.
