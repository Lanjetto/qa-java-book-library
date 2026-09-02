package library.api;

import jakarta.validation.Valid;
import library.api.dto.BookDto;
import library.api.dto.CreateBookRequest;
import library.api.dto.UpdateBookRequest;
import library.model.Book;
import library.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST-контроллер книг.
 * CRUD: GET /api/books[?search=], GET/PATCH/DELETE /api/books/{id}, POST /api/books.
 */
@RestController
@RequestMapping("/api/books")
public class BookApi {

    private final BookService service;

    public BookApi(BookService service) {
        this.service = service;
    }

    @GetMapping
    public List<BookDto> list(@RequestParam(value = "search", required = false) String search) {
        List<Book> books = (search == null || search.isBlank())
                ? service.findAll()
                : service.search(search);
        return books.stream().map(BookDto::from).toList();
    }

    @GetMapping("/{id}")
    public BookDto get(@PathVariable Long id) {
        return BookDto.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<BookDto> create(@Valid @RequestBody CreateBookRequest request) {
        Book created = service.createBook(request.toBook());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(BookDto.from(created));
    }

    @PatchMapping("/{id}")
    public BookDto update(@PathVariable Long id, @RequestBody UpdateBookRequest request) {
        return BookDto.from(service.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
