package library.api;

import io.restassured.RestAssured;
import library.api.dto.CreateBookRequest;
import library.testdata.BookMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static library.api.BookApiSpecs.okJson;
import static library.api.BookApiSpecs.requestSpec;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Интеграционный CRUD по HTTP на REST Assured (Б13, ветка t5).
 *
 * <p>В отличие от {@code BookApiContextTest} (ветка t4, TestRestTemplate на H2), этот тест поднимает
 * приложение на {@link org.springframework.boot.web.server.WebServerApplicationContext} с **настоящим
 * Postgres** (профиль по умолчанию из application.yml → {@code docker-compose.yml} в WSL / Testcontainers)
 * и ходит через REST Assured DSL: {@code given().when().then()} читается как предложение.
 *
 * <p>Тест помечен {@code @Tag("docker")} — в дефолтный {@code ./gradlew test} не входит и запускается
 * таской {@code integrationTest} (см. §7.3 HANDOFF). HTTP-тест ходит «наружу», поэтому {@code @Transactional}
 * тут не спасает: сервер коммитит сам. Изоляцию данных обеспечиваем уникальными ISBN
 * ({@link BookMother#unique()}) и удалением созданного в {@code @AfterEach}.
 */
@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookCrudTest {

    @LocalServerPort
    private int port;

    /** id книг, созданных тестами, — чистим после каждого теста (Б14: «создал сам — сам убрал»). */
    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        // Логируем запрос/ответ только когда проверка падает — чистый вывод в зелёном прогоне.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    void cleanUp() {
        for (Long id : created) {
            given().when().delete("/api/books/{id}", id);   // best-effort: тесты уже могли удалить
        }
        created.clear();
    }

    /** Создание книги через API. Возвращает id из ответа; id запоминается для очистки. */
    private Integer createBook(CreateBookRequest request) {
        Integer id = given(requestSpec())
                .body(request)
                .when()
                .post("/api/books")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
        created.add(id.longValue());
        return id;
    }

    @Test
    @DisplayName("CRUD: создать 201 → прочитать → PATCH → удалить 204 → после удаления 404")
    void createReadPatchDelete() {
        CreateBookRequest request = BookMother.unique();

        Integer id = createBook(request);

        // чтение созданного — поля совпадают с тем, что отправили
        given(requestSpec())
                .when().get("/api/books/{id}", id)
                .then().spec(okJson())
                .body("isbn", equalTo(request.isbn()))
                .body("title", equalTo(request.title()))
                .body("author", equalTo(request.author()))
                .body("status", equalTo("AVAILABLE"));

        // PATCH меняет только переданные поля
        given(requestSpec())
                .body(Map.of("status", "SOLD"))
                .when().patch("/api/books/{id}", id)
                .then()
                .statusCode(200)
                .body("status", equalTo("SOLD"))
                .body("title", equalTo(request.title()));   // не трогали — не изменилось

        // DELETE → 204, повторный GET → 404
        given(requestSpec()).when().delete("/api/books/{id}", id)
                .then().statusCode(204);
        given(requestSpec()).when().get("/api/books/{id}", id)
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/books/{id} несуществующей книги → 404 + ErrorResponseDto с путём")
    void getMissingBookReturns404ErrorDto() {
        given(requestSpec())
                .when().get("/api/books/{id}", 9_999_999_999L)
                .then()
                .statusCode(404)
                .body("path", equalTo("/api/books/9999999999"))
                .body("message", containsString("9999999999"));
    }

    @Test
    @DisplayName("POST без обязательного поля (автор пустой) → 400 от bean-валидации")
    void createWithoutRequiredFieldReturns400() {
        Map<String, Object> invalid = Map.of(
                "isbn", BookMother.uniqueIsbn(),
                "title", "Без автора",
                "author", "",
                "year", 2024,
                "price", 100.0,
                "status", "AVAILABLE");

        given(requestSpec())
                .body(invalid)
                .when().post("/api/books")
                .then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("message", containsString("author"));
    }

    @Test
    @DisplayName("GET /api/books возвращает созданную книгу (список)")
    void listContainsCreatedBook() {
        CreateBookRequest request = BookMother.unique();
        Integer id = createBook(request);

        given(requestSpec())
                .when().get("/api/books")
                .then().spec(okJson())
                .body("id", hasItem(id));
    }
}
