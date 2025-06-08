package crypto.insight.crypto.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "api.mobula")
public class MobulaProperties {
    
    private final String key;
    private final String baseUrl;
    
    @ConstructorBinding
    public MobulaProperties(String key, String baseUrl) {
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
        return "MobulaProperties{" +
                "key='" + (key != null ? "***" : "null") + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                '}';
    }
}