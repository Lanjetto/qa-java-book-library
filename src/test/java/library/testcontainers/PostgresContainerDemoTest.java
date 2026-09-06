package library.testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Минимальный «витринный» пример Testcontainers (Б15): автономный, **без Spring**.
 * Аннотационный режим {@code @Testcontainers} + {@code @Container} на {@code static} поле —
 * расширение JUnit (модуль {@code junit-jupiter}) стартует контейнер один раз перед классом
 * и останавливает после него.
 *
 * <p>Почему здесь аннотации уместны, а в {@link AbstractPostgresIT} — нет: Spring TestContext
 * кэширует application-context между тест-классами, а расширение останавливает контейнер в конце
 * класса → «контекст переживает контейнер», следующий класс падает. В этом тесте Spring-контекста
 * нет, поэтому жизненный цикл можно спокойно отдать расширению.
 *
 * <p>{@code @Tag("docker")} — тест не входит в дефолтный {@code ./gradlew test}
 * (там исключены docker-теги), запуск — {@code ./gradlew integrationTest}.
 */
@Tag("docker")
@Testcontainers
class PostgresContainerDemoTest {

    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("demo")
                    .withUsername("demo")
                    .withPassword("demo");

    @Test
    @DisplayName("контейнер поднимает настоящий Postgres: JDBC видит продукт и БД demo")
    void connectsToRealPostgres() throws Exception {
        try (Connection c = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("select current_database()")) {

            assertThat(c.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("demo");   // withDatabaseName("demo") реально применён
        }
    }
}
