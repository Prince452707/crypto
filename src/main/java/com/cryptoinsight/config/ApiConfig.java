package com.cryptoinsight.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig {
    private CryptoCompare cryptocompare = new CryptoCompare();
    private CoinGecko coingecko = new CoinGecko();
    private Mobula mobula = new Mobula();

    @Data
    public static class CryptoCompare {
        private String key;
        private String baseUrl;
    }

    @Data
    public static class CoinGecko {
        private String baseUrl;
    }

    @Data
    public static class Mobula {
        private String key;
        private String baseUrl;
    }
}
