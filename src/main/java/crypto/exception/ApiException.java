package crypto.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final String provider;
    private final int statusCode;

    public ApiException(String message, String provider, int statusCode) {
        super(message);
        this.provider = provider;
        this.statusCode = statusCode;
    }
}
