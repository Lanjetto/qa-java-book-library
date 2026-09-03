package library.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Полный контекст (@SpringBootTest, Б12): приложение поднимается целиком — контроллеры,
 * сервис, JPA и Liquibase-миграции (профиль test → H2). Сквозной CRUD-сценарий по HTTP
 * (TestRestTemplate): именно этот тест — локальная замена ручному прогону с Postgres
 * и источник покрытия BookService для JaCoCo.
 *
 * Контекст один на класс и НЕ откатывает транзакции (HTTP идёт «наружу»), поэтому каждый
 * тест создаёт книги с уникальными ISBN — данные между тестами не конфликтуют.
 */
// t9 (Б19): Allure-лейблы — тот же Epic «Книги», что и у регресс-сценария, другая Feature.
@Epic("Книги")
@Feature("REST API: CRUD и поиск")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookApiContextTest {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final Pattern UTC_INSTANT =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z");

    @Autowired
    private TestRestTemplate rest;

    /** Уникальный в пределах прогона 13-значный ISBN (978 + 10 цифр). */
    private static String nextIsbn() {
        return String.format("978%010d", SEQ.getAndIncrement());
    }

    /** Создание книги через API. Возвращает id из Location/тела. */
    private Long createBook(String title) {
        Map<String, Object> body = Map.of(
                "isbn", nextIsbn(),
                "title", title,
                "author", "Роберт Мартин",
                "year", 2008,
                "price", 899.99,
                "status", "AVAILABLE");

        ResponseEntity<Map> resp = rest.postForEntity("/api/books", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        Object id = resp.getBody().get("id");
        assertThat(id).isNotNull();
        return ((Number) id).longValue();
    }

    private Map<String, Object> getBook(Long id) {
        ResponseEntity<Map> resp = rest.getForEntity("/api/books/{id}", Map.class, id);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    @Test
    @DisplayName("CRUD: создать 201 → прочитать → PATCH → удалить 204 → после удаления 404")
    void crudFlow() {
        Long id = createBook("Чистый код");

        // GET по id: поля + createdAt сериализуется в UTC (с «Z»)
        Map<String, Object> created = getBook(id);
        assertThat(created.get("title")).isEqualTo("Чистый код");
        assertThat(created.get("author")).isEqualTo("Роберт Мартин");
        assertThat(String.valueOf(created.get("createdAt"))).matches(UTC_INSTANT);

        // PATCH меняет только статус
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> patch = rest.exchange("/api/books/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "SOLD"), headers), Map.class, id);
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patch.getBody()).containsEntry("status", "SOLD")
                .containsEntry("title", "Чистый код"); // остальные поля не тронуты

        // DELETE → 204, повторный GET → 404
        ResponseEntity<Void> del = rest.exchange("/api/books/{id}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class, id);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/books/{id}", Map.class, id).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PATCH меняет переданные поля (title/author/year/price), статус не трогает")
    void patchUpdatesProvidedFields() {
        Long id = createBook("До правки");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> patchBody = Map.of(
                "title", "После правки",
                "author", "Uncle Bob",
                "year", 2009,
                "price", 1200.0);

        ResponseEntity<Map> patch = rest.exchange("/api/books/{id}", HttpMethod.PATCH,
                new HttpEntity<>(patchBody, headers), Map.class, id);

        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patch.getBody()).containsEntry("title", "После правки")
                .containsEntry("author", "Uncle Bob")
                .containsEntry("year", 2009)
                .containsEntry("price", 1200.0)
                // статус не передавали — остался прежним (AVAILABLE)
                .containsEntry("status", "AVAILABLE");
    }

    @Test
    @DisplayName("GET /api/books возвращает созданную книгу")
    void listContainsCreatedBook() {
        String title = "Список-тест " + SEQ.getAndIncrement();
        Long id = createBook(title);

        ResponseEntity<Map[]> list = rest.getForEntity("/api/books", Map[].class);

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).isNotNull();
        assertThat(list.getBody()).extracting(b -> ((Number) b.get("id")).longValue())
                .contains(id);
    }

    @Test
    @DisplayName("GET /api/books?search= ищет по подстроке названия (в т.ч. кириллица)")
    void searchFindsByTitle() {
        String token = "Атомная " + SEQ.getAndIncrement();
        createBook(token);

        // Значение передаём URI-переменной — RestTemplate сам сделает percent-кодирование UTF-8
        // (сырая кириллица в URL ломается, как и в curl, — см. операционные уроки).
        ResponseEntity<Map[]> list = rest.getForEntity(
                "/api/books?search={q}", Map[].class, token);

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).isNotNull();
        assertThat(list.getBody()).extracting(b -> (String) b.get("title"))
                .contains(token);
    }

    @Test
    @DisplayName("некорректный ISBN → 400 + ErrorResponseDto")
    void createWithInvalidIsbnReturns400() {
        Map<String, Object> body = Map.of(
                "isbn", "не-isbn",
                "title", "Плохой ISBN",
                "author", "Кто-то",
                "year", 2020,
                "price", 100.0,
                "status", "AVAILABLE");

        ResponseEntity<Map> resp = rest.postForEntity("/api/books", body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsKeys("status", "error", "message", "path");
        assertThat(String.valueOf(resp.getBody().get("error"))).contains("Bad Request");
    }

    @Test
    @DisplayName("отсутствующая книга → 404 + ErrorResponseDto с путём")
    void getMissingBookReturns404ErrorDto() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/books/{id}", Map.class, 99999999L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("path", "/api/books/99999999");
        assertThat(String.valueOf(resp.getBody().get("message"))).contains("99999999");
    }
}
