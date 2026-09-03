package library.messaging;

import library.model.Book;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Реальная публикация в RabbitMQ ({@code RabbitTemplate.convertAndSend} на exchange/routing из
 * {@code application.yml}, ключи {@code app.mq.*}). Не в профиле {@code test}: там контексты поднимаются
 * на H2 без брокера, и публикация была бы попыткой соединения в никуда (см. {@link NoopBookEventPublisher}).
 */
@Component
@Profile("!test")
public class RabbitBookEventPublisher implements BookEventPublisher {

    private final RabbitTemplate rabbit;
    private final String exchange;
    private final String createdRouting;

    public RabbitBookEventPublisher(RabbitTemplate rabbit,
                                    @Value("${app.mq.exchange}") String exchange,
                                    @Value("${app.mq.created-routing}") String createdRouting) {
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.createdRouting = createdRouting;
    }

    @Override
    public void publishCreated(Book book) {
        rabbit.convertAndSend(exchange, createdRouting,
                new BookCreatedEvent(book.getId(), book.getIsbn(), book.getTitle()));
    }
}
