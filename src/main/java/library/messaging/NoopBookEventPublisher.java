package library.messaging;

import library.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Заглушка публикации для профиля {@code test}: локальные H2-контексты ({@code @SpringBootTest},
 * {@code @DataJpaTest}) не поднимают брокер, поэтому реальная отправка (см. {@link RabbitBookEventPublisher})
 * превратилась бы в попытку соединения в никуда и 500 в сквозных тестах. Внешняя граница (RabbitMQ)
 * покрывается docker-тестами с контейнером Testcontainers.
 */
@Component
@Profile("test")
public class NoopBookEventPublisher implements BookEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopBookEventPublisher.class);

    @Override
    public void publishCreated(Book book) {
        log.debug("Профиль test: событие book.created для книги {} НЕ публикуется (брокер не поднят)", book.getId());
    }
}
