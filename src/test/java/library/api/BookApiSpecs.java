package library.api;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

/**
 * Переиспользуемые спецификации REST Assured (Б13.2): RequestSpecification — общие части запроса
 * (заголовки), ResponseSpecification — ожидания на ответ. Убирают дублирование contentType/статуса
 * из каждого теста; при изменении API правки делаются в одном месте.
 *
 * <p>baseURI/port задаются глобально (RestAssured.*) в @BeforeEach теста из @LocalServerPort,
 * поэтому здесь хватает только контракта: JSON туда-обратно и код 200.
 */
public final class BookApiSpecs {

    private BookApiSpecs() {
    }

    /** Общий запрос: посылаем и принимаем JSON. */
    public static RequestSpecification requestSpec() {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /** Ожидание «успешный ответ с JSON-телом». */
    public static ResponseSpecification okJson() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }
}
