package library.regression;

import library.testdata.BookMother;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регресс-сценарий (t8/Б17): полный жизненный цикл статусов книги AVAILABLE → RESERVED → SOLD.
 * Такой сценарий называют «регресс-задачей»: он проходит «по верхам» всех фич (создание, PATCH,
 * чтение, листинг, поиск) и ловит регресс — когда правка одной части сломала соседнюю.
 *
 * <p>Главное правило стабильного (не флаки) сценария — изоляция и независимость от порядка:
 * <ul>
 *   <li>книга создаётся с {@link BookMother#unique()} — ISBN уникален в пределах прогона
 *       (BookApiContextTest пишет в ту же in-memory H2, но ISBN у него свои);</li>
 *   <li>мы НИКОГДА не проверяем общее количество книг в листинге («всего N») — только наличие
 *       своей книги и её статус. Иначе тест упадёт, если до него кто-то создал/удалил книги;</li>
 *   <li>после PATCH всегда читаем объект заново и сверяемся с ожиданием на конкретном id.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookLifecycleRegressionTest {

    @Autowired
    private TestRestTemplate rest;

    private Long createBook() {
        // BookMother.unique() — CREATE-запрос со случайным (уникальным) ISBN и статусом AVAILABLE.
        ResponseEntity<Map> resp = rest.postForEntity("/api/books", BookMother.unique(), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Object id = resp.getBody().get("id");
        assertThat(id).isNotNull();
        return ((Number) id).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> book(Long id) {
        ResponseEntity<Map> resp = rest.getForEntity("/api/books/{id}", Map.class, id);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> patchStatus(Long id, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> resp = rest.exchange("/api/books/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", status), headers), Map.class, id);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    /** Ищет свою книгу (по id) в списке из GET /api/books / ?search= — не полагаясь на общее число книг. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findInList(List<?> rows, Long id, String where) {
        for (Object row : rows) {
            Map<String, Object> book = (Map<String, Object>) row;
            if (((Number) book.get("id")).longValue() == id) {
                return book;
            }
        }
        throw new AssertionError("Книга id=" + id + " не найдена: " + where);
    }

    @Test
    @DisplayName("жизненный цикл статусов AVAILABLE → RESERVED → SOLD: каждая смена видна в чтении/листинге/поиске")
    void fullStatusLifecycle() {
        Long id = createBook();

        // стартовое состояние — AVAILABLE
        assertThat(book(id)).containsEntry("status", "AVAILABLE");

        // читатель зарезервировал книгу
        assertThat(patchStatus(id, "RESERVED")).containsEntry("status", "RESERVED");
        assertThat(book(id)).containsEntry("status", "RESERVED");

        // книга продана
        assertThat(patchStatus(id, "SOLD")).containsEntry("status", "SOLD");
        assertThat(book(id)).containsEntry("status", "SOLD");

        // листинг отражает финальный статус именно нашей книги (не «всего N» — см. javadoc)
        ResponseEntity<List> list = rest.getForEntity("/api/books", List.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).isNotNull();
        Map<String, Object> ours = findInList(list.getBody(), id, "в листинге GET /api/books");
        assertThat(ours).containsEntry("status", "SOLD");

        // поиск тоже видит книгу в финальном статусе
        String title = (String) ours.get("title");
        ResponseEntity<List> search = rest.getForEntity(
                "/api/books?search={q}", List.class, title);
        assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> found = findInList(search.getBody(), id, "в поиске ?search=" + title);
        assertThat(found).containsEntry("status", "SOLD");

        // повторный PATCH того же статуса — идемпотентен и не роняет сценарий (стабильность)
        assertThat(patchStatus(id, "SOLD")).containsEntry("status", "SOLD");
    }
}
