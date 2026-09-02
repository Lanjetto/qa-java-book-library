package library.api;

import library.exception.BookNotFoundException;
import library.model.Author;
import library.model.Book;
import library.model.BookStatus;
import library.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Срез web-слоя (@WebMvcTest(BookApi.class), Б12): поднимается только контроллер
 * (+ @RestControllerAdvice), сервис мокается — тестируется сериализация, маршруты,
 * валидация и обработка ошибок без БД.
 *
 * Сервис мокается через @MockitoBean — это новый API вместо классического @MockBean
 * (в Boot 3.4+ переехал в org.springframework.test.context.bean.override.mockito).
 */
@WebMvcTest(BookApi.class)
class BookApiSliceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookService bookService;

    private Book book() {
        Book b = new Book(1L, "9780132350884", "Clean Code",
                new Author("Роберт Мартин", 1952), 2008,
                new BigDecimal("3000.00"), BookStatus.AVAILABLE);
        b.setCreatedAt(Instant.parse("2026-09-03T10:15:30Z"));
        return b;
    }

    @Test
    @DisplayName("GET /api/books/{id}: книга сериализуется в JSON, createdAt — UTC с Z")
    void getExistingBookReturnsJson() throws Exception {
        when(bookService.findById(1L)).thenReturn(book());

        mvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.author").value("Роберт Мартин"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-03T10:15:30Z"));
    }

    @Test
    @DisplayName("GET /api/books/{id} отсутствующей книги: сервис бросает 404 → ErrorResponseDto")
    void getMissingBookReturns404() throws Exception {
        when(bookService.findById(999L)).thenThrow(new BookNotFoundException(999L));

        mvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/books/999"))
                .andExpect(jsonPath("$.message").value("Книга с id=999 не найдена"));
    }

    @Test
    @DisplayName("POST /api/books с пустым isbn → 400 от bean-валидации")
    void createWithMissingIsbnReturns400() throws Exception {
        mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Без ISBN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
