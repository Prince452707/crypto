package crypto.insight.crypto.service;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ThrottleService {
    private static final ConcurrentHashMap<String, LocalDateTime> lastRequestTimes = new ConcurrentHashMap<>();
    private static final Duration THROTTLE_DURATION = Duration.ofSeconds(1);

    public void throttle(String key) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastRequestTime = lastRequestTimes.get(key);
        
        if (lastRequestTime != null) {
            Duration timeSinceLastRequest = Duration.between(lastRequestTime, now);
            if (timeSinceLastRequest.compareTo(THROTTLE_DURATION) < 0) {
                try {
                    Thread.sleep(THROTTLE_DURATION.minus(timeSinceLastRequest).toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Throttle interrupted", e);
                }
            }
        }
        
        lastRequestTimes.put(key, LocalDateTime.now());
    }
}
