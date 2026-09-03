package library.api;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-тест (t8/Б17): быстрая проверка «приложение живо и можно начинать прогон» — НЕ про полноту.
 * Полный контекст поднимается, и тест дёргает {@code /actuator/health} (Spring Boot Actuator).
 * Если приложение не стартует (конфигурация, Liquibase, бин-ошибка) — health не ответит, и smoke
 * красный раньше, чем побегут долгие сценарии.
 *
 * <p>{@code @Tag("smoke")} — пример тега (в JUnit 5 удобно запускать «быструю» выборку отдельно:
 * {@code ./gradlew test --tests '*LibrarySmokeTest'}); в дефолтный прогон он входит, в отличие от
 * {@code @Tag("docker")}. {@code /actuator/health} в SecurityConfig (профиль !test) — permitAll.
 */
@Epic("Книги")
@Feature("Живость приложения (Actuator)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("smoke")
class LibrarySmokeTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("smoke: /actuator/health отвечает 200 и status=UP — приложение живо")
    @Description("Быстрая проверка «можно ли начинать прогон»: полный контекст поднимается, "
            + "health отвечает 200/UP. Тег @Tag(\"smoke\") позволяет гонять выборку отдельно.")
    @Severity(SeverityLevel.CRITICAL)
    void healthIsUp() {
        ResponseEntity<Map> resp = rest.getForEntity("/actuator/health", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody())
                .isNotNull()
                .containsEntry("status", "UP");
    }
}
