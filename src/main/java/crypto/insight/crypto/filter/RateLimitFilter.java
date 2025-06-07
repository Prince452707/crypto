package crypto.insight.crypto.filter;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter implements WebFilter {
    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 20;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private static class RequestCounter {
        private int count = 0;
        private Instant windowStart = Instant.now();

        boolean tryAcquire() {
            Instant now = Instant.now();
            if (Duration.between(windowStart, now).compareTo(WINDOW) > 0) {
                count = 0;
                windowStart = now;
            }
            if (count < MAX_REQUESTS) {
                count++;
                return true;
            }
            return false;
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        RequestCounter counter = counters.computeIfAbsent(ip, k -> new RequestCounter());

        if (counter.tryAcquire()) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }
}
