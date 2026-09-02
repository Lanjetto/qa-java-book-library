package library.api;

import jakarta.servlet.http.HttpServletRequest;
import library.api.dto.ErrorResponseDto;
import library.exception.BookNotFoundException;
import library.exception.InvalidIsbnException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Единый обработчик ошибок API (@RestControllerAdvice):
 * доменные исключения → понятный HTTP-статус + ErrorResponseDto.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(BookNotFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.of(HttpStatus.NOT_FOUND, e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidIsbnException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidIsbn(InvalidIsbnException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(HttpStatus.BAD_REQUEST, e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Ошибка валидации");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleUnreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(HttpStatus.BAD_REQUEST, "Некорректное тело запроса", request.getRequestURI()));
    }
}
