package crypto.insight.crypto.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "api.cryptocompare")
public class CryptoCompareProperties {
    
    private final String key;
    private final String baseUrl;
    
    @ConstructorBinding
    public CryptoCompareProperties(String key, String baseUrl) {
        this.key = key;
        this.baseUrl = baseUrl;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    @Override
    public String toString() {
        return "CryptoCompareProperties{" +
                "key='" + (key != null ? "***" : "null") + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                '}';
    }
}
