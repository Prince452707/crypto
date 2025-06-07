// package crypto.insight.crypto.service;

// import com.google.cloud.vertexai.VertexAI;
// import com.google.cloud.vertexai.api.GenerateContentResponse;
// import com.google.cloud.vertexai.generativeai.GenerativeModel;
// import crypto.insight.crypto.model.ChartDataPoint;
// import crypto.insight.crypto.model.Cryptocurrency;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import javax.annotation.PostConstruct;
// import java.math.BigDecimal;
// import java.math.RoundingMode;
// import java.time.Instant;
// import java.time.LocalDateTime;
// import java.time.ZoneId;
// import java.time.format.DateTimeFormatter;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.CompletableFuture;
// import java.util.stream.Collectors;

// @Slf4j
// @Service
// public class AIService {
//     private GenerativeModel model;

//     @Value("${ai.gemini.project-id}")
//     private String projectId;

//     @Value("${ai.gemini.location}")
//     private String location;

//     @Value("${ai.gemini.model-name}")
//     private String modelName;

//     @PostConstruct
//     public void init() {
//         try {
//             VertexAI vertexAI = new VertexAI(projectId, location);
//             model = new GenerativeModel(modelName, vertexAI);
//         } catch (Exception e) {
//             log.error("Error initializing Vertex AI", e);
//             throw new RuntimeException("Failed to initialize Vertex AI", e);
//         }
//     }

//     public CompletableFuture<Map<String, String>> generateComprehensiveAnalysis(String contextData) {
//         Map<String, String> analysis = new HashMap<>();

//         return CompletableFuture.supplyAsync(() -> {
//             try {
//                 analysis.put("general", generateAnalysis("general", contextData));
//                 analysis.put("technical", generateAnalysis("technical", contextData));
//                 analysis.put("fundamental", generateAnalysis("fundamental", contextData));
//                 analysis.put("news", generateAnalysis("news", contextData));
//                 analysis.put("sentiment", generateAnalysis("sentiment", contextData));
//                 analysis.put("risk", generateAnalysis("risk", contextData));
//                 analysis.put("prediction", generateAnalysis("prediction", contextData));
//                 return analysis;
//             } catch (Exception e) {
//                 log.error("Error generating analysis", e);
//                 throw new RuntimeException(e);
//             }
//         });
//     }

//     private String generateAnalysis(String type, String contextData) {
//         try {
//             String prompt = buildPrompt(type, contextData);
//             GenerateContentResponse response = model.generateContent(prompt);
//             // Fix the candidates access
//             var candidates = response.getCandidatesList();
//             if (candidates != null && !candidates.isEmpty()) {
//                 var content = candidates.get(0).getContent();
//                 if (content != null && content.getPartsList() != null && !content.getPartsList().isEmpty()) {
//                     return content.getPartsList().get(0).getText();
//                 }
//             }
//             return "No analysis generated";
//         } catch (Exception e) {
//             log.error("Error generating {} analysis", type, e);
//             return "Error generating " + type + " analysis: " + e.getMessage();
//         }
//     }

//     private String buildPrompt(String type, String contextData) {
//         return switch (type) {
//             case "general" -> "Analyze general market trends for: " + contextData;
//             case "technical" -> "Provide technical analysis for: " + contextData;
//             case "fundamental" -> "Analyze fundamental factors for: " + contextData;
//             case "news" -> "Analyze recent news impact for: " + contextData;
//             case "sentiment" -> "Evaluate market sentiment for: " + contextData;
//             case "risk" -> "Assess investment risks for: " + contextData;
//             case "prediction" -> "Generate price predictions for: " + contextData;
//             default -> throw new IllegalArgumentException("Unknown analysis type: " + type);
//         };
//     }

//     public String buildAnalysisContext(
//             Cryptocurrency crypto,
//             Map<String, Object> details,
//             Map<String, Object> teamData,
//             List<List<Number>> priceData,
//             List<Map<String, String>> news,
//             int days) {
        
//         // Convert priceData to ChartDataPoints
//         List<ChartDataPoint> chartDataPoints = convertToChartDataPoints(priceData);
//         Map<String, Double> metrics = calculateMetrics(chartDataPoints);
        
//         // Convert news to Map<String, Object> to match formatContextData parameters
//         Map<String, Object> newsData = new HashMap<>();
//         newsData.put("news", news);
        
//         return formatContextData(
//             crypto,
//             details,
//             newsData, // Pass the wrapped news data
//             teamData,
//             Collections.emptyList(),
//             chartDataPoints,
//             metrics,
//             days
//         );
//     }

//     private List<ChartDataPoint> convertToChartDataPoints(List<List<Number>> priceData) {
//         if (priceData == null) return List.of();
//         return priceData.stream()
//             .map(point -> new ChartDataPoint(
//                 ((Number) point.get(0)).longValue(),
//                 ((Number) point.get(1)).doubleValue()
//             ))
//             .collect(Collectors.toList());
//     }

//     private Map<String, Double> calculateMetrics(List<ChartDataPoint> priceData) {
//         Map<String, Double> metrics = new HashMap<>();
        
//         // Convert BigDecimal to Double when putting into map
//         BigDecimal volatility = calculateVolatility(priceData);
//         BigDecimal sevenDayAvg = calculateAverage(priceData, 7);
//         BigDecimal thirtyDayAvg = calculateAverage(priceData, 30);
        
//         metrics.put("volatility", volatility.doubleValue());
//         metrics.put("sevenDayAvg", sevenDayAvg.doubleValue());
//         metrics.put("thirtyDayAvg", thirtyDayAvg.doubleValue());
        
//         return metrics;
//     }

//     private BigDecimal calculateVolatility(List<ChartDataPoint> priceData) {
//         if (priceData.size() < 2) return BigDecimal.ZERO;

//         List<BigDecimal> returns = new ArrayList<>();
//         for (int i = 1; i < priceData.size(); i++) {
//             BigDecimal current = BigDecimal.valueOf(priceData.get(i).getPrice());
//             BigDecimal previous = BigDecimal.valueOf(priceData.get(i-1).getPrice());
//             returns.add(current.subtract(previous).divide(previous, 6, RoundingMode.HALF_UP));
//         }

//         return calculateStandardDeviation(returns)
//             .multiply(BigDecimal.valueOf(100))
//             .multiply(BigDecimal.valueOf(Math.sqrt(365))); // Annualized volatility
//     }

//     private BigDecimal calculateStandardDeviation(List<BigDecimal> values) {
//         BigDecimal mean = values.stream()
//             .reduce(BigDecimal.ZERO, BigDecimal::add)
//             .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);

//         BigDecimal variance = values.stream()
//             .map(value -> value.subtract(mean).pow(2))
//             .reduce(BigDecimal.ZERO, BigDecimal::add)
//             .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);

//         return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
//     }

//     private BigDecimal calculateAverage(List<ChartDataPoint> priceData, int days) {
//         int start = Math.max(0, priceData.size() - days);
//         List<ChartDataPoint> subList = priceData.subList(start, priceData.size());

//         BigDecimal sum = subList.stream()
//             .map(row -> BigDecimal.valueOf(row.getPrice()))
//             .reduce(BigDecimal.ZERO, BigDecimal::add);

//         return sum.divide(BigDecimal.valueOf(subList.size()), 6, RoundingMode.HALF_UP);
//     }

//     private String formatTeamData(Map<String, Object> teamData) {
//         if (teamData == null || teamData.isEmpty()) {
//             return "No team data available";
//         }

//         List<Map<String, Object>> teamMembers = (List<Map<String, Object>>) teamData.get("team");
//         if (teamMembers == null || teamMembers.isEmpty()) {
//             return "No team members available";
//         }

//         return teamMembers.stream()
//             .limit(3)
//             .map(member -> String.format("- %s: %s",
//                 member.get("name") != null ? member.get("name") : "N/A",
//                 member.get("position") != null ? member.get("position") : "N/A"))
//             .collect(Collectors.joining("\n"));
//     }

//     private String formatNewsData(List<Map<String, String>> news) {
//         if (news == null || news.isEmpty()) {
//             return "No news available";
//         }

//         return news.stream()
//             .limit(5)
//             .map(item -> String.format("- %s (%s)",
//                 item.get("title") != null ? item.get("title") : "N/A",
//                 item.get("source") != null ? item.get("source") : "N/A"))
//             .collect(Collectors.joining("\n"));
//     }

//     private String buildRealDataContext(
//             Cryptocurrency crypto,
//             Map<String, Object> cryptoDetails,
//             Map<String, Object> newsData,
//             Map<String, Object> teamData,
//             List<Cryptocurrency> btcEthData,
//             List<ChartDataPoint> priceData,
//             int days) {

//         return formatContextData(
//             crypto, cryptoDetails, newsData, teamData,
//             btcEthData, priceData, calculateMetrics(priceData), days
//         );
//     }

//     private double calculateVolatility(List<ChartDataPoint> priceData) {
//         if (priceData.size() < 2) return 0.0;
//         List<Double> returns = new ArrayList<>();
//         for (int i = 1; i < priceData.size(); i++) {
//             double currentPrice = priceData.get(i).getPrice();
//             double previousPrice = priceData.get(i-1).getPrice();
//             returns.add((currentPrice - previousPrice) / previousPrice * 100);
//         }
//         return Math.sqrt(calculateVariance(returns));
//     }

//     private double calculateVariance(List<Double> values) {
//         double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
//         double variance = values.stream()
//             .mapToDouble(value -> Math.pow(value - mean, 2))
//             .average()
//             .orElse(0.0);
//         return variance;
//     }

//     private String formatContextData(
//             Cryptocurrency crypto,
//             Map<String, Object> cryptoDetails,
//             Map<String, Object> newsData,
//             Map<String, Object> teamData,
//             List<Cryptocurrency> btcEthData,
//             List<ChartDataPoint> priceData,
//             Map<String, Double> metrics,
//             int days) {

//         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//         String currentDate = LocalDateTime.now().format(formatter);

//         // Ensure marketData is not null
//         Map<String, Object> marketData = (Map<String, Object>) cryptoDetails.get("market_data");
//         if (marketData == null) {
//             marketData = new HashMap<>();
//         }

//         // Ensure btcEthData is not null and has at least two elements
//         if (btcEthData == null || btcEthData.size() < 2) {
//             btcEthData = List.of(
//                 Cryptocurrency.builder().price(BigDecimal.ZERO).percentChange24h(BigDecimal.ZERO).build(),
//                 Cryptocurrency.builder().price(BigDecimal.ZERO).percentChange24h(BigDecimal.ZERO).build()
//             );
//         }

//         return String.format("""
//             REAL DATA CONTEXT for %s (%s) - %s

//             CURRENT MARKET DATA:
//             - Current Price: $%.2f
//             - Market Cap: $%.2f
//             - Rank: %s
//             - 24h Volume: $%.2f
//             - 24h Change: %.2f%%
//             - 7d Change: %s%%
//             - 30d Change: %s%%
//             - Circulating Supply: %s
//             - Max Supply: %s

//             CALCULATED METRICS:
//             - 7-day Average Price: $%.2f
//             - 30-day Average Price: $%.2f
//             - Volatility (%d days): %.2f%%
//             - ATH: $%s
//             - ATL: $%s
//             - ATH Change: %s%%

//             PRICE HISTORY (%d data points over %d days):
//             %s

//             RECENT NEWS:
//             %s

//             TEAM INFORMATION:
//             %s

//             PROJECT INFO:
//             - Description: %s
//             - Categories: %s
//             - Website: %s

//             COMPARISON DATA:
//             - Bitcoin: $%.2f (%.2f%%)
//             - Ethereum: $%.2f (%.2f%%)
//             """,
//             crypto.getName(),
//             crypto.getSymbol(),
//             currentDate,
//             crypto.getPrice(),
//             crypto.getMarketCap(),
//             marketData.get("market_cap_rank") != null ? marketData.get("market_cap_rank").toString() : "N/A",
//             crypto.getVolume24h(),
//             crypto.getPercentChange24h(),
//             marketData.get("price_change_percentage_7d") != null ? marketData.get("price_change_percentage_7d").toString() : "N/A",
//             marketData.get("price_change_percentage_30d") != null ? marketData.get("price_change_percentage_30d").toString() : "N/A",
//             marketData.get("circulating_supply") != null ? marketData.get("circulating_supply").toString() : "N/A",
//             marketData.get("max_supply") != null ? marketData.get("max_supply").toString() : "N/A",
//             metrics.get("sevenDayAvg"),
//             metrics.get("thirtyDayAvg"),
//             days,
//             metrics.get("volatility"),
//             marketData.get("ath") != null ? marketData.get("ath").toString() : "N/A",
//             marketData.get("atl") != null ? marketData.get("atl").toString() : "N/A",
//             marketData.get("ath_change_percentage") != null ? marketData.get("ath_change_percentage").toString() : "N/A",
//             priceData.size(),
//             days,
//             formatPriceHistory(priceData),
//             formatNewsData(news),
//             formatTeamData(teamData),
//             truncateDescription((String) cryptoDetails.get("description"), 200),
//             cryptoDetails.get("categories") != null ? String.join(", ", (List<String>) cryptoDetails.get("categories")) : "N/A",
//             cryptoDetails.get("website") != null ? cryptoDetails.get("website").toString() : "N/A",
//             btcEthData.get(0).getPrice(),
//             btcEthData.get(0).getPercentChange24h(),
//             btcEthData.get(1).getPrice(),
//             btcEthData.get(1).getPercentChange24h()
//         );
//     }

//     private String formatPriceHistory(List<ChartDataPoint> priceData) {
//         if (priceData == null || priceData.isEmpty()) {
//             return "No price history available";
//         }

//         return priceData.stream()
//             .limit(10)
//             .map(point -> String.format("%s: $%.2f",
//                 Instant.ofEpochMilli(point.getTimestamp())
//                     .atZone(ZoneId.systemDefault())
//                     .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
//                 point.getPrice()))
//             .collect(Collectors.joining("\n"));
//     }

//     private String truncateDescription(String description, int maxLength) {
//         return description != null ?
//             (description.length() > maxLength ?
//                 description.substring(0, maxLength) + "..." :
//                 description) :
//             "N/A";
//     }
// }










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
        } catch (Exception e) {
            log.error("Error initializing Vertex AI", e);
            throw new RuntimeException("Failed to initialize Vertex AI", e);
        }
    }

    public CompletableFuture<AnalysisResponse> generateComprehensiveAnalysis(
            Cryptocurrency crypto,
            List<ChartDataPoint> chartDataPoints,
            int days) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> analysis = new HashMap<>();
                String contextData = buildAnalysisContext(crypto, chartDataPoints, days);
                analysis.put("general", generateAnalysis("general", contextData));
                analysis.put("technical", generateAnalysis("technical", contextData));
                analysis.put("fundamental", generateAnalysis("fundamental", contextData));
                analysis.put("news", generateAnalysis("news", contextData));
                analysis.put("sentiment", generateAnalysis("sentiment", contextData));
                analysis.put("risk", generateAnalysis("risk", contextData));
                analysis.put("prediction", generateAnalysis("prediction", contextData));
                return new AnalysisResponse(analysis);
            } catch (Exception e) {
                log.error("Error generating analysis for {}: {}", crypto != null ? crypto.getSymbol() : "null", e.getMessage(), e);
                throw new RuntimeException("Failed to generate analysis", e);
            }
        });
    }

    private String generateAnalysis(String type, String contextData) {
        try {
            String prompt = buildPrompt(type, contextData);
            GenerateContentResponse response = model.generateContent(prompt);
            var candidates = response.getCandidatesList();
            if (candidates != null && !candidates.isEmpty()) {
                var content = candidates.get(0).getContent();
                if (content != null && !content.getPartsList().isEmpty()) {
                    return content.getPartsList().get(0).getText();
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
            case "general" -> "Analyze general market trends for: " + contextData;
            case "technical" -> "Provide technical analysis for: " + contextData;
            case "fundamental" -> "Analyze fundamental factors for: " + contextData;
            case "news" -> "Analyze recent news impact for: " + contextData;
            case "sentiment" -> "Evaluate market sentiment for: " + contextData;
            case "risk" -> "Assess investment risks for: " + contextData;
            case "prediction" -> "Generate price predictions for: " + contextData;
            default -> throw new IllegalArgumentException("Unknown analysis type: " + type);
        };
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
        BigDecimal volatility = calculateVolatility(priceData);
        BigDecimal sevenDayAvg = calculateAverage(priceData, 7);
        BigDecimal thirtyDayAvg = calculateAverage(priceData, 30);
        metrics.put("volatility", volatility.doubleValue());
        metrics.put("sevenDayAvg", sevenDayAvg.doubleValue());
        metrics.put("thirtyDayAvg", thirtyDayAvg.doubleValue());
        return metrics;
    }

    private BigDecimal calculateVolatility(List<ChartDataPoint> priceData) {
        if (priceData == null || priceData.size() < 2) return BigDecimal.ZERO;
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < priceData.size(); i++) {
            BigDecimal current = BigDecimal.valueOf(priceData.get(i).getPrice());
            BigDecimal previous = BigDecimal.valueOf(priceData.get(i - 1).getPrice());
            returns.add(current.subtract(previous).divide(previous, 6, RoundingMode.HALF_UP));
        }
        return calculateStandardDeviation(returns)
                .multiply(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(Math.sqrt(365))); // Annualized volatility
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal mean = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
        BigDecimal variance = values.stream()
                .map(value -> value.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    private BigDecimal calculateAverage(List<ChartDataPoint> priceData, int days) {
        if (priceData == null || priceData.isEmpty()) return BigDecimal.ZERO;
        int start = Math.max(0, priceData.size() - days);
        List<ChartDataPoint> subList = priceData.subList(start, priceData.size());
        BigDecimal sum = subList.stream()
                .map(row -> BigDecimal.valueOf(row.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(subList.size()), 6, RoundingMode.HALF_UP);
    }

    private String formatPriceHistory(List<ChartDataPoint> priceData) {
        if (priceData == null || priceData.isEmpty()) {
            return "No price history available";
        }
        return priceData.stream()
                .limit(10)
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
        BigDecimal percentChange24h =冥

System: safeBigDecimal(crypto.getPercentChange24h());
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

                PRICE HISTORY (%d data points over %d days):
                %s
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
                circulatingSupply.toString(),
                priceData.size(),
                days,
                formatPriceHistory(priceData)
        );
    }

    private String safeString(Object obj) {
        return obj != null ? obj.toString() : "N/A";
    }

    private BigDecimal safeBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal: {}", obj);
            return BigDecimal.ZERO;
        }
    }
}