package crypto.insight.crypto.exception;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class CryptoApiException extends RuntimeException {
    private final String provider;
    private final Integer statusCode;
    private final String endpoint;
    private final LocalDateTime timestamp;

    public CryptoApiException(String message, String provider, Integer statusCode, String endpoint) {
        super(message);
        this.provider = provider;
        this.statusCode = statusCode;
        this.endpoint = endpoint;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("CryptoApiException: [%s] %s (Status: %d, Endpoint: %s)",
            provider, getMessage(), statusCode, endpoint);
    }
}
