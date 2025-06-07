package crypto.insight.crypto.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cryptocurrency {
    private String id;
    private String name;
    private String symbol;
    private BigDecimal price;
    private BigDecimal marketCap;
    private BigDecimal volume24h;
    private BigDecimal percentChange24h;
    private String image;
    private int rank;
    private BigDecimal circulatingSupply;
    private BigDecimal totalSupply;
    private BigDecimal maxSupply;

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getMarketCap() { return marketCap; }
    public BigDecimal getVolume24h() { return volume24h; }
    public BigDecimal getPercentChange24h() { return percentChange24h; }
    public String getImage() { return image; }
    public int getRank() { return rank; }
    public BigDecimal getCirculatingSupply() { return circulatingSupply; }
    public BigDecimal getTotalSupply() { return totalSupply; }
    public BigDecimal getMaxSupply() { return maxSupply; }
}
