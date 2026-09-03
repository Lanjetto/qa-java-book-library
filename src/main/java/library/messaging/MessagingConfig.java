package library.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Декларация RabbitMQ-инфраструктуры (t7/Б16): exchange + очередь + привязка по routing-ключу,
 * а также JSON-конвертер сообщений (иначе record не сериализуется — SimpleMessageConverter требует
 * Serializable). Объявляется автоматически при старте (RabbitAdmin) — отдельно «создавать» в брокере
 * ничего не нужно. Не в профиле {@code test}: там брокера нет, а очередь слушателя поднимается только
 * с ним (см. listener).
 */
@Configuration
@Profile("!test")
public class MessagingConfig {

    @Bean
    TopicExchange bookEventsExchange(@Value("${app.mq.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);   // durable
    }

    @Bean
    Queue bookCreatedQueue(@Value("${app.mq.created-queue}") String queue) {
        return new Queue(queue, true);                     // durable
    }

    @Bean
    Binding bookCreatedBinding(Queue bookCreatedQueue, TopicExchange bookEventsExchange,
                               @Value("${app.mq.created-routing}") String routing) {
        return BindingBuilder.bind(bookCreatedQueue).to(bookEventsExchange).with(routing);
    }

    /** События в очереди — JSON (Jackson), а не Java-сериализация. */
    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
