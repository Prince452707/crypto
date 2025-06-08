// package crypto.config;

// import org.springframework.boot.context.properties.ConfigurationProperties;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpHeaders;

// import java.util.Map;
// import java.util.HashMap;

// @Configuration
// @ConfigurationProperties(prefix = "crypto.compare")
// public class CryptoApiConfig {

//     // Properties for CryptoCompare API
//     @Value("${crypto.compare.base-url:https://min-api.cryptocompare.com}")
//     private String baseUrl;

//     @Value("${crypto.compare.api-key:}")
//     private String apiKey;

//     @Value("${crypto.compare.timeout:30000}")
//     private int timeout;

//     // Default headers - no longer using problematic SpEL expression
//     private Map<String, String> headers = new HashMap<>();

//     // Initialize default headers in constructor
//     public CryptoApiConfig() {
//         this.headers.put("Content-Type", "application/json");
//         this.headers.put("Accept", "application/json");
//         this.headers.put("User-Agent", "CryptoInsight/1.0");
//     }

//     @Bean
//     public WebClient cryptoCompareWebClient() {
//         WebClient.Builder builder = WebClient.builder()
//                 .baseUrl(baseUrl)
//                 .defaultHeaders(httpHeaders -> {
//                     headers.forEach(httpHeaders::add);
//                     // Add API key if provided
//                     if (apiKey != null && !apiKey.isEmpty()) {
//                         httpHeaders.add("Authorization", "Apikey " + apiKey);
//                     }
//                 });

//         return builder.build();
//     }

//     @Bean
//     public WebClient genericWebClient() {
//         return WebClient.builder()
//                 .defaultHeaders(httpHeaders -> {
//                     httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
//                     httpHeaders.add(HttpHeaders.ACCEPT, "application/json");
//                     httpHeaders.add(HttpHeaders.USER_AGENT, "CryptoInsight/1.0");
//                 })
//                 .build();
//     }

//     // Getters and Setters
//     public String getBaseUrl() {
//         return baseUrl;
//     }

//     public void setBaseUrl(String baseUrl) {
//         this.baseUrl = baseUrl;
//     }

//     public String getApiKey() {
//         return apiKey;
//     }

//     public void setApiKey(String apiKey) {
//         this.apiKey = apiKey;
//     }

//     public int getTimeout() {
//         return timeout;
//     }

//     public void setTimeout(int timeout) {
//         this.timeout = timeout;
//     }

//     public Map<String, String> getHeaders() {
//         return headers;
//     }

//     public void setHeaders(Map<String, String> headers) {
//         this.headers = headers;
//     }
// }






package crypto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;

@Configuration
public class MultiApiConfig {

    // CryptoCompare API Configuration
    @Value("${api.cryptocompare.key:}")
    private String cryptoCompareApiKey;

    @Value("${api.cryptocompare.base-url}")
    private String cryptoCompareBaseUrl;

    // CoinGecko API Configuration
    @Value("${api.coingecko.base-url}")
    private String coinGeckoBaseUrl;

    // CoinPaprika API Configuration
    @Value("${api.coinpaprika.base-url}")
    private String coinPaprikaBaseUrl;

    // Mobula API Configuration
    @Value("${api.mobula.key:}")
    private String mobulaApiKey;

    @Value("${api.mobula.base-url}")
    private String mobulaBaseUrl;

    // Gemini AI Configuration
    @Value("${ai.gemini.project-id:}")
    private String geminiProjectId;

    @Value("${ai.gemini.location:}")
    private String geminiLocation;

    @Value("${ai.gemini.model-name:}")
    private String geminiModelName;

    // CryptoCompare WebClient
    @Bean("cryptoCompareWebClient")
    public WebClient cryptoCompareWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(cryptoCompareBaseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
                    httpHeaders.add(HttpHeaders.ACCEPT, "application/json");
                    httpHeaders.add(HttpHeaders.USER_AGENT, "CryptoInsight/1.0");
                    // Add API key if provided
                    if (cryptoCompareApiKey != null && !cryptoCompareApiKey.isEmpty()) {
                        httpHeaders.add("Authorization", "Apikey " + cryptoCompareApiKey);
                    }
                });

        return builder.build();
    }

    // CoinGecko WebClient
    @Bean("coinGeckoWebClient")
    public WebClient coinGeckoWebClient() {
        return WebClient.builder()
                .baseUrl(coinGeckoBaseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
                    httpHeaders.add(HttpHeaders.ACCEPT, "application/json");
                    httpHeaders.add(HttpHeaders.USER_AGENT, "CryptoInsight/1.0");
                })
                .build();
    }

    // CoinPaprika WebClient
    @Bean("coinPaprikaWebClient")
    public WebClient coinPaprikaWebClient() {
        return WebClient.builder()
                .baseUrl(coinPaprikaBaseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
                    httpHeaders.add(HttpHeaders.ACCEPT, "application/json");
                    httpHeaders.add(HttpHeaders.USER_AGENT, "CryptoInsight/1.0");
                })
                .build();
    }

    // Mobula WebClient
    @Bean("mobulaWebClient")
    public WebClient mobulaWebClient() {
        return WebClient.builder()
                .baseUrl(mobulaBaseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
                    httpHeaders.add(HttpHeaders.ACCEPT, "application/json");
                    httpHeaders.add(HttpHeaders.USER_AGENT, "CryptoInsight/1.0");
                    // Add API key if provided
                    if (mobulaApiKey != null && !mobulaApiKey.isEmpty()) {
                        httpHeaders.add("Authorization", "Bearer " + mobulaApiKey);
                    }
                })
                .build();
    }

    // Generic WebClient for any other APIs
    @Bean("genericWebClient")
    public WebClient genericWebClient() {
        return WebClient.builder()
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
                    httpHeaders.add(HttpHeaders.ACCEPT, "application/json");
                    httpHeaders.add(HttpHeaders.USER_AGENT, "CryptoInsight/1.0");
                })
                .build();
    }

    // Getters for configuration properties (useful for services)
    public String getCryptoCompareApiKey() {
        return cryptoCompareApiKey;
    }

    public String getCryptoCompareBaseUrl() {
        return cryptoCompareBaseUrl;
    }

    public String getCoinGeckoBaseUrl() {
        return coinGeckoBaseUrl;
    }

    public String getCoinPaprikaBaseUrl() {
        return coinPaprikaBaseUrl;
    }

    public String getMobulaApiKey() {
        return mobulaApiKey;
    }

    public String getMobulaBaseUrl() {
        return mobulaBaseUrl;
    }

    public String getGeminiProjectId() {
        return geminiProjectId;
    }

    public String getGeminiLocation() {
        return geminiLocation;
    }

    public String getGeminiModelName() {
        return geminiModelName;
    }
}