package crypto.insight.crypto.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@Configuration
public class CryptoApiConfig {
    private static final Logger log = LoggerFactory.getLogger(CryptoApiConfig.class);

    // CryptoCompare API configuration
    @Value("${crypto.compare.api-key:682a577a63781a9dc6fc13cad64b4adcfe37e8b60d624de1e6a5fcd2423e94d6}")
    private String coinCompareApiKey;

    @Value("${crypto.compare.base-url:https://min-api.cryptocompare.com/data}")
    private String coinCompareBaseUrl;

    @Value("#{${crypto.compare.headers}}")
    private Map<String, String> coinCompareHeaders;

    // CoinGecko API configuration
    @Value("#{'${coin.gecko.base-urls}'.split(',')}")
    private List<String> coinGeckoBaseUrls;

    @Value("#{'${coin.gecko2.base-urls}'.split(',')}")
    private List<String> coinGecko2BaseUrls;

    // Mobula API configuration
    @Value("${mobula.api-key:a4b4be25-293a-41d0-b28a-f13035f648a5}")
    private String mobulaApiKey;

    @Value("${mobula.base-url:https://api.mobula.io/api/1}")
    private String mobulaBaseUrl;

    @Value("#{${mobula.headers}}")
    private Map<String, String> mobulaHeaders;

    // Coinpaprika API configuration
    @Value("${coinpaprika.base-url:https://api.coinpaprika.com/v1}")
    private String coinpaprikaBaseUrl;

    @Value("#{${coinpaprika.headers}}")
    private Map<String, String> coinpaprikaHeaders;

    // API request configurations
    @Value("${api.rate-limit-duration-ms:200}")
    private long rateLimitDurationMs;

    @Value("${api.standard-timeout-seconds:15}")
    private long standardTimeoutSeconds;

    @Value("${api.max-retries:3}")
    private int maxRetries;

    @Value("#{'${api.retry-status-codes:429,500,502,503,504}'.split(',')}")
    private List<Integer> retryStatusCodes;

    // Cache configuration
    @Value("${cache.default-duration-minutes:15}")
    private long defaultCacheDurationMinutes;

    @Value("${cache.market-data-duration-minutes:5}")
    private long marketDataCacheDurationMinutes;

    @Value("${cache.news-duration-minutes:10}")
    private long newsCacheDurationMinutes;

    @Value("${cache.details-duration-hours:2}")
    private long detailsCacheDurationHours;

    @Value("${cache.team-data-duration-hours:24}")
    private long teamDataCacheDurationHours;

    // Feature flags
    @Value("${feature.use-coin-gecko-fallback:true}")
    private boolean useCoinGeckoFallback;

    @Value("${feature.use-mobula-fallback:true}")
    private boolean useMobulaFallback;

    @Value("${feature.use-coinpaprika-team-data:true}")
    private boolean useCoinpaprikaTeamData;

    @Value("${feature.use-crypto-compare-news-data:true}")
    private boolean useCryptoCompareNewsData;

    // Error handling configuration
    @Value("${error.max-consecutive-errors:5}")
    private int maxConsecutiveErrors;

    @Value("${error.error-cooldown-period-minutes:5}")
    private long errorCooldownPeriodMinutes;

    // Exponential backoff parameters
    @Value("${backoff.initial-ms:1000}")
    private int initialBackoffMs;

    @Value("${backoff.multiplier:2.0}")
    private double backoffMultiplier;

    @Value("${backoff.max-ms:10000}")
    private int maxBackoffMs;

    @Value("${api.endpoint-strategy:ROUND_ROBIN}")
    private ApiEndpointStrategy endpointStrategy;

    // Duration getters
    public Duration getRateLimitDuration() {
        return Duration.ofMillis(rateLimitDurationMs);
    }

    public Duration getStandardTimeout() {
        return Duration.ofSeconds(standardTimeoutSeconds);
    }

    public Duration getDefaultCacheDuration() {
        return Duration.ofMinutes(defaultCacheDurationMinutes);
    }

    public Duration getMarketDataCacheDuration() {
        return Duration.ofMinutes(marketDataCacheDurationMinutes);
    }

    public Duration getNewsCacheDuration() {
        return Duration.ofMinutes(newsCacheDurationMinutes);
    }

    public Duration getDetailsCacheDuration() {
        return Duration.ofHours(detailsCacheDurationHours);
    }

    public Duration getTeamDataCacheDuration() {
        return Duration.ofHours(teamDataCacheDurationHours);
    }

    public Duration getErrorCooldownPeriod() {
        return Duration.ofMinutes(errorCooldownPeriodMinutes);
    }

    // API endpoint selection strategy
    public enum ApiEndpointStrategy {
        ROUND_ROBIN,
        RANDOM,
        LATENCY_BASED,
        FAILOVER
    }

    // Helper methods for headers
    public void initializeHeaders() {
        // CryptoCompare headers
        if (coinCompareHeaders == null) {
            coinCompareHeaders = Map.of(
                "Authorization", "Apikey " + coinCompareApiKey,
                "Content-Type", "application/json"
            );
        }

        // Mobula headers
        if (mobulaHeaders == null) {
            mobulaHeaders = Map.of(
                "Authorization", "Bearer " + mobulaApiKey,
                "Content-Type", "application/json"
            );
        }

        // Coinpaprika headers
        if (coinpaprikaHeaders == null) {
            coinpaprikaHeaders = Map.of(
                "Accept", "application/json"
            );
        }
    }

    @PostConstruct
    public void init() {
        initializeHeaders();
        validateConfiguration();
    }

    private void validateConfiguration() {
        if (coinCompareApiKey == null || coinCompareApiKey.isEmpty()) {
            log.warn("CryptoCompare API key not set. Some features may be limited.");
        }
        if (mobulaApiKey == null || mobulaApiKey.isEmpty()) {
            log.warn("Mobula API key not set. Some features may be limited.");
        }
        if (coinGeckoBaseUrls == null || coinGeckoBaseUrls.isEmpty()) {
            coinGeckoBaseUrls = List.of("https://api.coingecko.com/api/v3");
            log.info("Using default CoinGecko API URL");
        }
    }

    public Map<String, String> getCoinCompareHeaders() {
        if (coinCompareHeaders == null) {
            return Map.of(
                "Authorization", "Apikey " + coinCompareApiKey,
                "Content-Type", "application/json"
            );
        }
        return coinCompareHeaders;
    }

    public Map<String, String> getMobulaHeaders() {
        if (mobulaHeaders == null) {
            return Map.of(
                "Authorization", "Bearer " + mobulaApiKey,
                "Content-Type", "application/json"
            );
        }
        return mobulaHeaders;
    }

    public Map<String, String> getCoinpaprikaHeaders() {
        if (coinpaprikaHeaders == null) {
            return Map.of("Accept", "application/json");
        }
        return coinpaprikaHeaders;
    }
}
