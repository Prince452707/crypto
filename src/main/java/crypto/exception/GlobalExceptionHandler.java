package crypto.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), ex.getProvider(), ex.getStatusCode());
        return ResponseEntity.status(HttpStatus.valueOf(error.status())).body(error);
    }

    record ErrorResponse(String message, String provider, int status) {}
}
