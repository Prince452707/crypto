package com.cryptoinsight.service;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ThrottleService {
    private final ConcurrentHashMap<String, Instant> lastRequestTimes = new ConcurrentHashMap<>();
    private static final Duration THROTTLE_DURATION = Duration.ofSeconds(1);

    public void throttle(String key) throws InterruptedException {
        Instant now = Instant.now();
        Instant lastRequest = lastRequestTimes.get(key);
        
        if (lastRequest != null) {
            Duration timeSinceLastRequest = Duration.between(lastRequest, now);
            if (timeSinceLastRequest.compareTo(THROTTLE_DURATION) < 0) {
                Thread.sleep(THROTTLE_DURATION.minus(timeSinceLastRequest).toMillis());
            }
        }
        
        lastRequestTimes.put(key, Instant.now());
    }
}
