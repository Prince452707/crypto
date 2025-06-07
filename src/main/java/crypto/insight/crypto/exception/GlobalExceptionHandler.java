package crypto.insight.crypto.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CryptoApiException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleApiException(CryptoApiException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), ex.getProvider(), ex.getStatusCode());
        return Mono.just(ResponseEntity.status(error.status()).body(error));
    }

    record ErrorResponse(String message, String provider, int status) {}
}
