package library.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import library.model.Book;
import library.model.BookStatus;
import library.repository.BookRepository;
import library.service.BookService;
import library.testcontainers.AbstractPostgresIT;
import library.testdata.BookMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест эмиттера события (t7/Б16): брокер НЕ мокаем — поднимаем настоящий RabbitMQ
 * (Testcontainers, {@link AbstractPostgresIT}) и проверяем, что после создания книги событие
 * {@code book.created} реально ушло в exchange и доехало до очереди.
 *
 * <p>Проверяем «факт публикации и доставки» через отдельную probe-очередь, привязанную к тому же
 * exchange по тому же routing-ключу: основной лог-слушатель вычитывает сообщение из своей очереди,
 * поэтому считать его оттуда нельзя. {@code @Tag("docker")} → запуск таской {@code integrationTest}.
 */
@Tag("docker")
@SpringBootTest
class BookEventPublisherTest extends AbstractPostgresIT {

    private static final String PROBE_QUEUE = "q.book.created.probe";

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.created-routing}")
    private String routing;

    @AfterEach
    void cleanUp() {
        rabbitAdmin.deleteQueue(PROBE_QUEUE);
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("создание книги публикует BookCreatedEvent в RabbitMQ (реально доезжает до очереди)")
    void createdBookPublishesEventToRabbit() throws Exception {
        // probe-очередь на тот же exchange/routing — проверяем доставку независимо от лог-слушателя
        rabbitAdmin.declareQueue(new Queue(PROBE_QUEUE, true));
        rabbitAdmin.declareBinding(BindingBuilder
                .bind(new Queue(PROBE_QUEUE, true))
                .to(new TopicExchange(exchange))
                .with(routing));

        Book book = BookMother.book(null, "9781111111111", "Probe-книга", "Автор Probe",
                1970, 2020, "100.00", BookStatus.AVAILABLE);
        Book saved = bookService.createBook(book);

        // получаем сообщение из probe-очереди (ждём до 5 c)
        org.springframework.amqp.core.Message message = rabbit.receive(PROBE_QUEUE, 5000);
        assertThat(message).as("событие должно дойти до очереди").isNotNull();

        BookCreatedEvent event =
                objectMapper.readValue(message.getBody(), BookCreatedEvent.class);
        assertThat(event.id()).isEqualTo(saved.getId());
        assertThat(event.isbn()).isEqualTo("9781111111111");
        assertThat(event.title()).isEqualTo("Probe-книга");
    }
}
