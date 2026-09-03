package library.api;

import io.restassured.RestAssured;
import library.api.dto.CreateBookRequest;
import library.model.Book;
import library.repository.BookRepository;
import library.testcontainers.AbstractPostgresIT;
import library.testdata.BookMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static library.api.BookApiSpecs.requestSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * Тест консистентности времени (Б14.2, «timezone в БД расходится с локальным»).
 *
 * <p>На настоящем Postgres (контейнер Testcontainers, {@link AbstractPostgresIT}, {@code @Tag("docker")}):
 * колонка {@code created_at} объявлена как {@code TIMESTAMP WITH TIME ZONE}, {@link Book#getCreatedAt()} —
 * {@link Instant} (абсолютный момент в UTC), JSON-сериализация Instant даёт ISO-8601 с «Z». Сверяем,
 * что значение, ушедшее в JSON (прошло через БД и обратно), совпадает с тем, что реально лежит в БД, —
 * и что формат строки UTC, а не локальный пояс JVM/сервера.
 *
 * <p>Сравниваем моменты, а не строки (Instant против Instant); хрупкая привязка к локальной зоне
 * и {@code -Duser.timezone} тут не нужна.
 */
@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookDateConsistencyTest extends AbstractPostgresIT {

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository repository;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterEach
    void cleanUp() {
        for (Long id : created) {
            repository.deleteById(id);   // best-effort очистка «живой» БД
        }
        created.clear();
    }

    @Test
    @DisplayName("createdAt из API и из БД — один и тот же момент; формат в JSON — UTC (Z)")
    void createdAtIsSameInApiAndDb() {
        CreateBookRequest request = BookMother.unique();

        Integer id = given(requestSpec())
                .body(request)
                .when().post("/api/books")
                .then()
                .statusCode(201)
                // Jackson сериализует Instant в ISO-8601 UTC: миллисекунды опциональны, обязательно «Z»
                .body("createdAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z"))
                .extract().path("id");
        created.add(id.longValue());

        // значение из JSON (прошло туда-обратно через HTTP/Jackson)
        String apiValue = given(requestSpec())
                .when().get("/api/books/{id}", id)
                .then().statusCode(200)
                .extract().path("createdAt");

        // значение из БД (прямое чтение JPA-репозиторием)
        Instant dbValue = repository.findById(id.longValue()).orElseThrow().getCreatedAt();

        Instant apiInstant = Instant.parse(apiValue);
        assertThat(apiInstant).isEqualTo(dbValue);
        // createdAt проставлен при создании (@PrePersist) — близок к «сейчас», а не «эпоха»
        assertThat(dbValue).isNotNull().isCloseTo(Instant.now(), within(2, ChronoUnit.MINUTES));
    }
}
