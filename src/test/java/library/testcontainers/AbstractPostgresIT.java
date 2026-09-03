package library.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Общая инфраструктура docker-интеграционных тестов (ветка t6, Б15): один Postgres-контейнер
 * на весь {@code @Tag("docker")}-прогон. Идея Testcontainers — «не мокай и не эмулируй то, что можно
 * реально поднять»: H2 близка, но не равна Postgres (диалект, TIMESTAMPTZ), поэтому настоящий
 * контракт проверяем на реальной БД в контейнере.
 *
 * <p>Контейнер стартуем вручную один раз ({@code static { … }}) и НЕ отдаём жизненный цикл расширению
 * {@code @Testcontainers}/{@code @Container}: расширение останавливает static-контейнер после каждого
 * тест-класса, а Spring кэширует application-context между классами на старую (уже остановленную) БД.
 * При ручном старте контейнер живёт весь JVM-прогон (Testcontainers уберёт его по Ryuk при выходе),
 * поэтому все наследники видят одну и ту же живую БД. {@code @ServiceConnection} (Boot 3.1+) настраивает
 * {@code DataSource} под контейнер без ручного {@code @DynamicPropertySource}; динамический порт
 * исключает конфликты. Liquibase применяется в контейнер при старте контекста → схема как в проде.
 *
 * <p>Тесты, наследующие этот класс, помечены {@code @Tag("docker")} и в дефолтный {@code ./gradlew test}
 * не входят — запуск таской {@code integrationTest} там, где есть Docker (см. README ветки).
 */
public abstract class AbstractPostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("library")
                    .withUsername("library")
                    .withPassword("library");

    static {
        POSTGRES.start();
    }
}
