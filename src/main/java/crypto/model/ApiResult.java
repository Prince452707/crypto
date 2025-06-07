package crypto.model;

import crypto.exception.CryptoApiException;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ApiResult<T> {
    private final T data;
    private final String error;
    private final String provider;
    private final boolean success;
    private final LocalDateTime timestamp;

    private ApiResult(T data, String error, String provider, boolean success) {
        this.data = data;
        this.error = error;
        this.provider = provider;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResult<T> success(T data, String provider) {
        return new ApiResult<>(data, null, provider, true);
    }

    public static <T> ApiResult<T> error(String error, String provider) {
        return new ApiResult<>(null, error, provider, false);
    }

    public T getDataOrThrow() {
        if (success && data != null) {
            return data;
        }
        throw new CryptoApiException(
            error != null ? error : "No data available",
            provider,
            null,
            "unknown"
        );
    }
}
