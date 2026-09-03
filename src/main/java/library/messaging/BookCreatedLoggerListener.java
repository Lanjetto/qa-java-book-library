package library.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Потребитель события (t7/Б16): в демо — слушатель-логгер, показывающий, что сообщение дошло до очереди.
 * Не в профиле {@code test}: без брокера listener-контейнер не поднять (покрывается docker-тестами).
 */
@Component
@Profile("!test")
public class BookCreatedLoggerListener {

    private static final Logger log = LoggerFactory.getLogger(BookCreatedLoggerListener.class);

    @RabbitListener(queues = "${app.mq.created-queue}")
    public void onBookCreated(BookCreatedEvent event) {
        log.info("Получено событие book.created: id={}, isbn={}, title={}", event.id(), event.isbn(), event.title());
    }
}
