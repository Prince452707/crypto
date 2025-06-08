
package crypto.insight.crypto.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import crypto.insight.crypto.model.AnalysisResponse;
import crypto.insight.crypto.model.ChartDataPoint;
import crypto.insight.crypto.model.Cryptocurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    // Constants
    private static final int MAX_PRICE_HISTORY_ITEMS = 10;
    private static final int PRECISION_SCALE = 6;
    private static final int ANNUALIZATION_DAYS = 365;
    private static final String DEFAULT_VALUE = "N/A";
    
    private GenerativeModel model;

    @Value("${ai.gemini.project-id}")
    private String projectId;

    @Value("${ai.gemini.location}")
    private String location;

    @Value("${ai.gemini.model-name}")
    private String modelName;

    @PostConstruct
    public void init() {
        try {
            VertexAI vertexAI = new VertexAI(projectId, location);
            model = new GenerativeModel(modelName, vertexAI);
            log.info("Vertex AI initialized successfully with project: {}, location: {}, model: {}", 
                    projectId, location, modelName);
        } catch (Exception e) {
            log.error("Error initializing Vertex AI - check configuration", e);
            throw new RuntimeException("Failed to initialize Vertex AI", e);
        }
    }

    public CompletableFuture<AnalysisResponse> generateComprehensiveAnalysis(
            Cryptocurrency crypto,
            List<ChartDataPoint> chartDataPoints,
            int days) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateInputs(crypto, chartDataPoints, days);
                
                Map<String, String> analysis = new HashMap<>();
                String contextData = buildAnalysisContext(crypto, chartDataPoints, days);
                
                // Generate different types of analysis
                analysis.put("general", generateAnalysis("general", contextData));
                analysis.put("technical", generateAnalysis("technical", contextData));
                analysis.put("fundamental", generateAnalysis("fundamental", contextData));
                analysis.put("news", generateAnalysis("news", contextData));
                analysis.put("sentiment", generateAnalysis("sentiment", contextData));
                analysis.put("risk", generateAnalysis("risk", contextData));
                analysis.put("prediction", generateAnalysis("prediction", contextData));
                
                return AnalysisResponse.builder()
    .analysis(analysis)
    .chartData(chartDataPoints)
    .build();
            } catch (Exception e) {
                log.error("Error generating analysis for {}: {}", 
                         crypto != null ? crypto.getSymbol() : "null", e.getMessage(), e);
                throw new RuntimeException("Failed to generate analysis", e);
            }
        });
    }

    private void validateInputs(Cryptocurrency crypto, List<ChartDataPoint> chartDataPoints, int days) {
        if (crypto == null) {
            throw new IllegalArgumentException("Cryptocurrency data cannot be null");
        }
        if (crypto.getSymbol() == null || crypto.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Cryptocurrency symbol cannot be null or empty");
        }
        if (days <= 0 || days > 365) {
            throw new IllegalArgumentException("Days must be between 1 and 365");
        }
        if (chartDataPoints == null) {
            log.warn("Chart data points are null for {}", crypto.getSymbol());
        }
    }

    private String generateAnalysis(String type, String contextData) {
        try {
            String prompt = buildPrompt(type, contextData);
            GenerateContentResponse response = model.generateContent(prompt);
            
            var candidates = response.getCandidatesList();
            if (candidates != null && !candidates.isEmpty()) {
                var content = candidates.get(0).getContent();
                if (content != null && !content.getPartsList().isEmpty()) {
                    String result = content.getPartsList().get(0).getText();
                    log.debug("Generated {} analysis successfully", type);
                    return result;
                }
            }
            
            log.warn("No {} analysis generated for context", type);
            return "No " + type + " analysis generated";
        } catch (Exception e) {
            log.error("Error generating {} analysis: {}", type, e.getMessage(), e);
            return "Error generating " + type + " analysis: " + e.getMessage();
        }
    }

    private String buildPrompt(String type, String contextData) {
        return switch (type) {
            case "general" -> buildGeneralPrompt(contextData);
            case "technical" -> buildTechnicalPrompt(contextData);
            case "fundamental" -> buildFundamentalPrompt(contextData);
            case "news" -> buildNewsPrompt(contextData);
            case "sentiment" -> buildSentimentPrompt(contextData);
            case "risk" -> buildRiskPrompt(contextData);
            case "prediction" -> buildPredictionPrompt(contextData);
            default -> throw new IllegalArgumentException("Unknown analysis type: " + type);
        };
    }

    private String buildGeneralPrompt(String contextData) {
        return "Provide a comprehensive general market analysis for the following cryptocurrency data. " +
               "Focus on overall market trends, position in the market, and key highlights: " + contextData;
    }

    private String buildTechnicalPrompt(String contextData) {
        return "Perform a detailed technical analysis including price patterns, support/resistance levels, " +
               "moving averages, volatility analysis, and trading signals for: " + contextData;
    }

    private String buildFundamentalPrompt(String contextData) {
        return "Analyze the fundamental factors including market cap analysis, supply economics, " +
               "adoption metrics, and long-term value proposition for: " + contextData;
    }

    private String buildNewsPrompt(String contextData) {
        return "Analyze the potential impact of recent market news and developments on this cryptocurrency. " +
               "Consider regulatory changes, partnerships, and market sentiment: " + contextData;
    }

    private String buildSentimentPrompt(String contextData) {
        return "Evaluate the current market sentiment and investor behavior patterns. " +
               "Analyze volume trends, price action, and market psychology for: " + contextData;
    }

    private String buildRiskPrompt(String contextData) {
        return "Provide a comprehensive risk assessment including volatility risks, market risks, " +
               "regulatory risks, and liquidity risks for: " + contextData;
    }

    private String buildPredictionPrompt(String contextData) {
        return "Generate informed price predictions and potential scenarios based on technical and " +
               "fundamental analysis. Include short-term and medium-term outlook for: " + contextData;
    }

    public String buildAnalysisContext(
            Cryptocurrency crypto,
            List<ChartDataPoint> chartDataPoints,
            int days) {
        
        if (crypto == null) {
            log.warn("Cryptocurrency is null in buildAnalysisContext");
            return "No cryptocurrency data available";
        }
        
        Map<String, Double> metrics = calculateMetrics(chartDataPoints);
        return formatContextData(crypto, chartDataPoints, metrics, days);
    }

    private Map<String, Double> calculateMetrics(List<ChartDataPoint> priceData) {
        Map<String, Double> metrics = new HashMap<>();
        
        if (priceData == null || priceData.isEmpty()) {
            log.warn("Price data is null or empty, returning default metrics");
            return getDefaultMetrics();
        }
        
        try {
            BigDecimal volatility = calculateVolatility(priceData);
            BigDecimal sevenDayAvg = calculateAverage(priceData, 7);
            BigDecimal thirtyDayAvg = calculateAverage(priceData, 30);
            
            metrics.put("volatility", volatility.doubleValue());
            metrics.put("sevenDayAvg", sevenDayAvg.doubleValue());
            metrics.put("thirtyDayAvg", thirtyDayAvg.doubleValue());
            
            // Additional metrics
            metrics.put("priceChange", calculatePriceChange(priceData));
            metrics.put("highLowRatio", calculateHighLowRatio(priceData));
            
        } catch (Exception e) {
            log.error("Error calculating metrics: {}", e.getMessage(), e);
            return getDefaultMetrics();
        }
        
        return metrics;
    }

    private Map<String, Double> getDefaultMetrics() {
        Map<String, Double> defaults = new HashMap<>();
        defaults.put("volatility", 0.0);
        defaults.put("sevenDayAvg", 0.0);
        defaults.put("thirtyDayAvg", 0.0);
        defaults.put("priceChange", 0.0);
        defaults.put("highLowRatio", 1.0);
        return defaults;
    }

    private double calculatePriceChange(List<ChartDataPoint> priceData) {
        if (priceData.size() < 2) return 0.0;
        
        double firstPrice = priceData.get(0).getPrice();
        double lastPrice = priceData.get(priceData.size() - 1).getPrice();
        
        return firstPrice != 0 ? ((lastPrice - firstPrice) / firstPrice) * 100 : 0.0;
    }

    private double calculateHighLowRatio(List<ChartDataPoint> priceData) {
        if (priceData.isEmpty()) return 1.0;
        
        double high = priceData.stream().mapToDouble(ChartDataPoint::getPrice).max().orElse(0.0);
        double low = priceData.stream().mapToDouble(ChartDataPoint::getPrice).min().orElse(0.0);
        
        return low != 0 ? high / low : 1.0;
    }

    private BigDecimal calculateVolatility(List<ChartDataPoint> priceData) {
        if (priceData == null || priceData.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < priceData.size(); i++) {
            BigDecimal current = BigDecimal.valueOf(priceData.get(i).getPrice());
            BigDecimal previous = BigDecimal.valueOf(priceData.get(i - 1).getPrice());
            
            if (previous.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal dailyReturn = current.subtract(previous)
                        .divide(previous, PRECISION_SCALE, RoundingMode.HALF_UP);
                returns.add(dailyReturn);
            }
        }
        
        return calculateStandardDeviation(returns)
                .multiply(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(Math.sqrt(ANNUALIZATION_DAYS))); // Annualized volatility
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Calculate mean
        BigDecimal mean = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), PRECISION_SCALE, RoundingMode.HALF_UP);
        
        // Calculate variance
        BigDecimal variance = values.stream()
                .map(value -> value.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), PRECISION_SCALE, RoundingMode.HALF_UP);
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    private BigDecimal calculateAverage(List<ChartDataPoint> priceData, int days) {
        if (priceData == null || priceData.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        int start = Math.max(0, priceData.size() - days);
        List<ChartDataPoint> subList = priceData.subList(start, priceData.size());
        
        BigDecimal sum = subList.stream()
                .map(row -> BigDecimal.valueOf(row.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(subList.size()), PRECISION_SCALE, RoundingMode.HALF_UP);
    }

    private String formatPriceHistory(List<ChartDataPoint> priceData) {
        if (priceData == null || priceData.isEmpty()) {
            return "No price history available";
        }
        
        return priceData.stream()
                .limit(MAX_PRICE_HISTORY_ITEMS)
                .map(point -> String.format("%s: $%.2f",
                        Instant.ofEpochMilli(point.getTimestamp())
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        point.getPrice()))
                .collect(Collectors.joining("\n"));
    }

    private String formatContextData(
            Cryptocurrency crypto,
            List<ChartDataPoint> priceData,
            Map<String, Double> metrics,
            int days) {
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentDate = LocalDateTime.now().format(formatter);

        // Safe access to crypto fields
        String name = safeString(crypto.getName());
        String symbol = safeString(crypto.getSymbol());
        BigDecimal price = safeBigDecimal(crypto.getPrice());
        BigDecimal marketCap = safeBigDecimal(crypto.getMarketCap());
        BigDecimal volume24h = safeBigDecimal(crypto.getVolume24h());
        BigDecimal percentChange24h = safeBigDecimal(crypto.getPercentChange24h());
        String rank = safeString(crypto.getRank());
        BigDecimal circulatingSupply = safeBigDecimal(crypto.getCirculatingSupply());

        return String.format("""
                ANALYSIS CONTEXT for %s (%s) - %s

                CURRENT MARKET DATA:
                - Current Price: $%.2f
                - Market Cap: $%.2f
                - Rank: %s
                - 24h Volume: $%.2f
                - 24h Change: %.2f%%
                - 7d Average Price: $%.2f
                - 30d Average Price: $%.2f
                - Volatility (%d days): %.2f%%
                - Circulating Supply: %s
                - Price Change (Period): %.2f%%
                - High/Low Ratio: %.2f

                PRICE HISTORY (%d data points over %d days):
                %s

                ADDITIONAL METRICS:
                - Data Quality: %s
                - Analysis Timestamp: %s
                """,
                name,
                symbol,
                currentDate,
                price.doubleValue(),
                marketCap.doubleValue(),
                rank,
                volume24h.doubleValue(),
                percentChange24h.doubleValue(),
                metrics.getOrDefault("sevenDayAvg", 0.0),
                metrics.getOrDefault("thirtyDayAvg", 0.0),
                days,
                metrics.getOrDefault("volatility", 0.0),
                formatSupply(circulatingSupply),
                metrics.getOrDefault("priceChange", 0.0),
                metrics.getOrDefault("highLowRatio", 1.0),
                priceData != null ? priceData.size() : 0,
                days,
                formatPriceHistory(priceData),
                priceData != null && !priceData.isEmpty() ? "Good" : "Limited",
                currentDate
        );
    }

    private String formatSupply(BigDecimal supply) {
        if (supply == null || supply.compareTo(BigDecimal.ZERO) == 0) {
            return DEFAULT_VALUE;
        }
        
        // Format large numbers with appropriate suffixes
        double value = supply.doubleValue();
        if (value >= 1_000_000_000) {
            return String.format("%.2fB", value / 1_000_000_000);
        } else if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000);
        } else if (value >= 1_000) {
            return String.format("%.2fK", value / 1_000);
        } else {
            return String.format("%.2f", value);
        }
    }

    private String safeString(Object obj) {
        return obj != null ? obj.toString() : DEFAULT_VALUE;
    }

    private BigDecimal safeBigDecimal(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            if (obj instanceof BigDecimal) {
                return (BigDecimal) obj;
            }
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal: {}", obj);
            return BigDecimal.ZERO;
        }
    }
}