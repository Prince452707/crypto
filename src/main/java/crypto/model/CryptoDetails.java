package crypto.model;

import lombok.Data;
import java.util.Map;

@Data
public class CryptoDetails {
    private final String id;
    private final String name;
    private final String symbol;
    private final String description;
    private final Map<String, Object> marketData;
    private final Map<String, Object> links;
    private final int marketCapRank;

    public CryptoDetails(Map<String, Object> details) {
        this.id = (String) details.get("id");
        this.name = (String) details.get("name");
        this.symbol = (String) details.get("symbol");
        this.description = (String) details.get("description");
        this.marketData = (Map<String, Object>) details.get("market_data");
        this.links = (Map<String, Object>) details.get("links");
        this.marketCapRank = (Integer) details.get("market_cap_rank");
    }
}
