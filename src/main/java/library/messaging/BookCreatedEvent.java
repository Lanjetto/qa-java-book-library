package library.messaging;

/**
 * Событие «книга создана» (t7/Б16), уходит в RabbitMQ как JSON (Jackson сериализует record).
 * Продюсер не ждёт ответа потребителя — это асинхронная интеграция.
 */
public record BookCreatedEvent(Long id, String isbn, String title) {
}
