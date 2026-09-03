import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.csv;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Нагрузочный сценарий Gatling (t10/Б20) — «минимальный честный»:
 * чтение списка и поиск по названию. Запускается против запущенного приложения.
 *
 * <p>Правила честной нагрузки:
 * <ul>
 *   <li>{@code check(status().is(200))} у каждого запроса — иначе успехом считается даже 500;</li>
 *   <li>фидер {@code csv(...).circular()} — разные поисковые строки, а не одна (кэш/одна ветка);</li>
 *   <li>{@code pause} — think time, без пауз сценарий нереалистично жжёт сервер;</li>
 *   <li>в {@code assertions} — перцентиль p99 и процент успеха: нарушение валит сборку красным.</li>
 * </ul>
 *
 * <p>Перед запуском подними приложение и (по желанию) наполни данными, затем:
 * <pre>
 *   ./gradlew bootRun                 # в отдельном терминале (или docker compose up + bootRun)
 *   ./gradlew gatlingRun              # в этом терминале
 *   ./gradlew gatlingRun -DbaseUrl=http://localhost:8080   # если порт/URL другой
 * </pre>
 * Отчёт: {@code build/reports/gatling/…/index.html} (Response Time Distribution).
 */
public class BookSearchSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    // t7+: /api/** защищён basic auth admin/secret (application.yml); в профиле test auth не нужен,
    // лишний заголовок не мешает.
    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .basicAuth("admin", "secret");

    // Фидер: разные поисковые строки (не одна и та же — чтобы не бить по одной ветке/кэшу).
    FeederBuilder<String> queries = csv("data/search-queries.csv").circular();

    ScenarioBuilder readScenario = scenario("Чтение и поиск книг")
            .feed(queries)
            .exec(http("GET /api/books — список")
                    .get("/api/books")
                    .check(status().is(200)))
            .pause(1, 2)      // think time между действиями пользователя
            .exec(http("GET /api/books?search=…")
                    .get("/api/books?search=#{query}")
                    .check(status().is(200)));

    {
        setUp(readScenario.injectOpen(
                        // load-фаза ramp-up: 20 виртуальных пользователей нарастают за 10 c
                        rampUsers(20).during(10)
                ))
                .protocols(httpProtocol)
                .assertions(
                        // p99 (перцентиль 4) < 1500 мс — «хвост», а не среднее
                        global().responseTime().percentile4().lt(1500),
                        // успешных запросов — больше 99 %
                        global().successfulRequests().percent().gt(99.0)
                );
    }
}
