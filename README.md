# book-library — демо «библиотека книг» (ветка `t7-security-rabbitmq`)

Ветка-контрольная точка `t7` добавляет две «настоящие» интеграции (Б16): **Spring Security (basic auth)**
для `/api/**` и событие **`book.created`** в RabbitMQ при создании книги. Тестируются авторизация (401/403)
и факт реальной публикации события — брокер не мокается (правило Б15).

## 1. Что уже есть (из `t6`)

- REST + JPA/Liquibase; Testcontainers (`AbstractPostgresIT`: Postgres + RabbitMQ на весь docker-прогон);
  REST Assured e2e `BookCrudTest`, timezone-тест, `BookRepositoryPostgresTest`.

## 2. Что добавлено в `t7`

- **Spring Security** (`spring-boot-starter-security`): `library/config/SecurityConfig.java` —
  `SecurityFilterChain` для `/api/**` (httpBasic, CSRF off, `/actuator/health` — permitAll под будущий t8);
  учётка `admin/secret` из `application.yml` (`spring.security.user.*`). Профиль `test` (H2-контексты) — open.
- **RabbitMQ** (`spring-boot-starter-amqp`): `library/messaging/`
  - `BookCreatedEvent(id, isbn, title)` — record, в очереди сериализуется JSON (`Jackson2JsonMessageConverter`);
  - интерфейс `BookEventPublisher` + `RabbitBookEventPublisher` (реальный `convertAndSend`, не в профиле `test`)
    и `NoopBookEventPublisher` (профиль `test`: без брокера H2-контексты не падают);
  - `MessagingConfig` — exchange `library.events`, очередь `q.book.created`, binding по `book.created`;
  - `BookCreatedLoggerListener` — демо-потребитель (лог).
  - `BookService.createBook` после сохранения публикует событие через `BookEventPublisher` (сервис зависит
    от интерфейса, не от RabbitTemplate).
- **docker-compose.yml**: добавлен сервис `rabbitmq:3-management` (5672 + 15672).
- **Тесты:**
  - авторизация: web-слайс `BookApiSliceTest` — `@WithMockUser` (+ `.with(csrf())` для POST: в `@WebMvcTest`
    работает дефолтный security с CSRF); e2e `BookCrudTest` ходит с `.auth().preemptive().basic("admin","secret")`
    и проверяет **401 без авторизации**;
  - `BookEventPublisherTest` (docker): probe-очередь на тот же exchange/routing, создание книги →
    `rabbit.receive(...)` ловит JSON-событие из настоящего RabbitMQ-контейнера;
  - `BookDateConsistencyTest`/`BookRepositoryPostgresTest` — обновлены под basic auth/контейнеры.

## 3. Как запустить / проверить

```
./gradlew build              # без Docker: юниты + H2-слайсы; docker-теги не трогает
./gradlew integrationTest    # с Docker: Postgres + RabbitMQ в контейнерах (10 тестов)
```
Запуск руками (compose в WSL):
```
docker compose up -d postgres rabbitmq
./gradlew bootRun            # http://localhost:8080
curl -s http://localhost:8080/api/books                     # → 401
curl -s -u admin:secret http://localhost:8080/api/books     # → 200
# в логе bootRun после создания книги: «Получено событие book.created: …»
```
> **Где проверено:** `./gradlew build` локально (Windows, ✅); `./gradlew integrationTest` — в WSL2
> с Docker (✅ 10 тестов, включая 401 и доставку события в RabbitMQ).

## 4. Задание «сделай сам» (Б16)

1. Добавь роль «админ»/«user» и тест 403: `user` не может, например, DELETE книгу.
2. Напиши второго потребителя события (своя очередь/логика) или прочитай сообщение из `q.book.created`
   через Management UI (http://localhost:15672).
3. Объясни, почему продюсер не ждёт ответа потребителя и как проверить, что событие реально ушло
   (не замокано).

## 5. Следующая ветка

`t8-smoke-regression` — Spring Boot Actuator (`/actuator/health`), smoke/регресс-сценарий по всем статусам
книги и параллельный запуск тестов (Б17).
