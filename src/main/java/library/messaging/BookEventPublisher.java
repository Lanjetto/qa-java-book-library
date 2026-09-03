package library.messaging;

import library.model.Book;

/**
 * Публикация события о создании книги (t7/Б16). Интерфейс — чтобы {@link library.service.BookService}
 * зависел от границы, а не от RabbitTemplate; реализация зависит от профиля:
 * {@link RabbitBookEventPublisher} (прод/стенд, реальный брокер) и {@link NoopBookEventPublisher}
 * (профиль test — H2-контексты без брокера не падают).
 */
public interface BookEventPublisher {

    void publishCreated(Book book);
}
