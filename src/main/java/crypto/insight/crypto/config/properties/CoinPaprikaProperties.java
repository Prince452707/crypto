package crypto.insight.crypto.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "api.coinpaprika")
public class CoinPaprikaProperties {
    
    private final String baseUrl;
    
    @ConstructorBinding
    public CoinPaprikaProperties(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
}