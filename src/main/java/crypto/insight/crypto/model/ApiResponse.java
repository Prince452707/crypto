package crypto.insight.crypto.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;
    private final String error;

    private ApiResponse(boolean success, String message, T data, String error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, "Error", null, error);
    }
}
