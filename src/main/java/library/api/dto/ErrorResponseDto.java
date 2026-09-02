package library.api.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Единый формат ошибок API.
 * Возвращается из @RestControllerAdvice (см. ApiExceptionHandler).
 */
public record ErrorResponseDto(Instant timestamp, int status, String error, String message, String path) {

    public static ErrorResponseDto of(HttpStatus status, String message, String path) {
        return new ErrorResponseDto(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
