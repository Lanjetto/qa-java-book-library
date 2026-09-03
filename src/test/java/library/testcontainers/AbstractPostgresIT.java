package library.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Общая инфраструктура docker-интеграционных тестов (ветки t6/t7): один Postgres-контейнер
 * и один RabbitMQ-контейнер на весь {@code @Tag("docker")}-прогон. Идея Testcontainers —
 * «не мокай и не эмулируй то, что можно реально поднять»: H2 близка, но не равна Postgres
 * (диалект, TIMESTAMPTZ), а событие {@code book.created} должно дойти до настоящего брокера.
 *
 * <p>Контейнеры стартуем вручную один раз ({@code static { … }}) и НЕ отдаём жизненный цикл расширению
 * {@code @Testcontainers}/{@code @Container}: расширение останавливает static-контейнер после каждого
 * тест-класса, а Spring кэширует application-context между классами на старую (уже остановленную) БД.
 * При ручном старте контейнеры живут весь JVM-прогон (Testcontainers уберёт их по Ryuk при выходе),
 * поэтому все наследники видят одну живую инфраструктуру. {@code @ServiceConnection} (Boot 3.1+)
 * настраивает и DataSource, и AMQP под контейнеры без ручного {@code @DynamicPropertySource};
 * динамические порты исключают конфликты. Liquibase применяется в контейнер при старте контекста.
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

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"));

    static {
        POSTGRES.start();
        RABBITMQ.start();
    }
}
