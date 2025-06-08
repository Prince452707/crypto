// package crypto.model;

// import lombok.Data;
// import java.util.Map;

// @Data
// public class CryptoDetails {
//     private final String id;
//     private final String name;
//     private final String symbol;
//     private final String description;
//     private final Map<String, Object> marketData;
//     private final Map<String, Object> links;
//     private final int marketCapRank;

//     public CryptoDetails(Map<String, Object> details) {
//         this.id = (String) details.get("id");
//         this.name = (String) details.get("name");
//         this.symbol = (String) details.get("symbol");
//         this.description = (String) details.get("description");
//         this.marketData = (Map<String, Object>) details.get("market_data");
//         this.links = (Map<String, Object>) details.get("links");
//         this.marketCapRank = (Integer) details.get("market_cap_rank");
//     }
// }






package crypto.model;

import lombok.Data;
import java.util.HashMap;
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

    @SuppressWarnings("unchecked")
    public CryptoDetails(Map<String, Object> details) {
        this.id = (String) details.getOrDefault("id", "");
        this.name = (String) details.getOrDefault("name", "");
        this.symbol = (String) details.getOrDefault("symbol", "");
        this.description = (String) details.getOrDefault("description", "");
        
        // Handle potential null values safely
        Object marketDataObj = details.get("market_data");
        this.marketData = (marketDataObj instanceof Map) ? 
                (Map<String, Object>) marketDataObj : new HashMap<>();
        
        Object linksObj = details.get("links");
        this.links = (linksObj instanceof Map) ? 
                (Map<String, Object>) linksObj : new HashMap<>();
        
        // Handle potential ClassCastException for market_cap_rank
        Object rankObj = details.get("market_cap_rank");
        if (rankObj instanceof Integer) {
            this.marketCapRank = (Integer) rankObj;
        } else if (rankObj instanceof Number) {
            this.marketCapRank = ((Number) rankObj).intValue();
        } else {
            this.marketCapRank = 0;
        }
    }
}
