package crypto.config.properties;
import crypto.config.properties.CoinGeckoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "api.coingecko")
public class CoinGeckoProperties {
    
    private final String baseUrl;
    
    @ConstructorBinding
    public CoinGeckoProperties(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
}