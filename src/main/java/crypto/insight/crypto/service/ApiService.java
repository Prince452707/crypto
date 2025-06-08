package crypto.insight.crypto.service;

import crypto.insight.crypto.config.ApiConfig;
import crypto.insight.crypto.model.Cryptocurrency;
import crypto.insight.crypto.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiService {
    private final WebClient webClient;
    private final ApiConfig apiConfig;

    @Cacheable(value = "cryptoData", key = "#symbol")
    public Mono<Cryptocurrency> getCryptocurrencyData(String symbol) {
        return webClient.get()
                .uri(apiConfig.getCryptocompare().getBaseUrl() + "/pricemultifull?fsyms=" + symbol + "&tsyms=USD")
                .header("Authorization", "Apikey " + apiConfig.getCryptocompare().getKey())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToCryptocurrency)
                .onErrorResume(e -> {
                    log.warn("Error fetching data from CryptoCompare for symbol {}: {}", symbol, e.getMessage());
                    return getFallbackData(symbol);
                });
    }

    private Mono<Cryptocurrency> getFallbackData(String symbol) {
        return webClient.get()
                .uri(apiConfig.getCoingecko().getBaseUrl() + "/simple/price?ids=" + symbol + "&vs_currencies=usd")
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToCryptocurrency)
                .onErrorResume(e -> {
                    log.error("Error fetching fallback data for symbol {}: {}", symbol, e.getMessage());
                    return Mono.just(null);
                });
    }

    @Cacheable(value = "marketChart", key = "#symbol + #days")
    public Mono<List<List<Number>>> getMarketChart(String symbol, int days) {
        return webClient.get()
                .uri(apiConfig.getCryptocompare().getBaseUrl() + "/v2/histoday?fsym=" + symbol + "&tsym=USD&limit=" + days)
                .header("Authorization", "Apikey " + apiConfig.getCryptocompare().getKey())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToMarketChart)
                .onErrorResume(e -> {
                    log.warn("Error fetching market chart from CryptoCompare for symbol {}: {}", symbol, e.getMessage());
                    return getMarketChartFallback(symbol, days);
                });
    }

    @Cacheable(value = "cryptoList")
    public Mono<List<Cryptocurrency>> getCryptocurrencies(int page, int perPage) {
        return getCryptoCompareData(page, perPage)
                .switchIfEmpty(getCoinGeckoData(page, perPage))
               // .switchIfEmpty(getMobulaData(page, perPage))
                .onErrorResume(e -> {
                    log.error("Error fetching cryptocurrency list: {}", e.getMessage(), e);
                    return Mono.just(Collections.emptyList());
                });
    }

    @Cacheable(value = "cryptoDetails", key = "#id")
    public Mono<Map<String, Object>> getCryptocurrencyDetails(String id) {
        return getDetailedData(id)
                .switchIfEmpty(getFallbackDetailedData(id))
                .onErrorResume(e -> {
                    log.error("Error fetching cryptocurrency details for id {}: {}", id, e.getMessage(), e);
                    return Mono.just(Collections.emptyMap());
                });
    }

    @Cacheable(value = "teamData", key = "#id")
    public Mono<Map<String, Object>> getTeamData(String id) {
        return webClient.get()
                .uri(apiConfig.getCoinpaprika().getBaseUrl() + "/coins/" + id + "/teams")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorResume(e -> {
                    log.warn("Error fetching team data for id {}: {}", id, e.getMessage());
                    return Mono.just(Collections.emptyMap());
                });
    }

    @Cacheable(value = "cryptoNews", key = "#symbol")
    public Mono<List<Map<String, String>>> getCombinedCryptoData(String symbol, String id) {
        return getCryptoCompareNews(symbol)
                .switchIfEmpty(getCoinGeckoNews(id))
                .onErrorResume(e -> {
                    log.error("Error fetching news data for symbol {}: {}", symbol, e.getMessage(), e);
                    return Mono.just(Collections.emptyList());
                });
    }

    private Mono<List<Cryptocurrency>> getCryptoCompareData(int page, int perPage) {
        return webClient.get()
                .uri(apiConfig.getCryptocompare().getBaseUrl() + "/top/mktcapfull?limit={limit}&tsym=USD&page={page}",
                        perPage, page - 1)
                .header("Authorization", "Apikey " + apiConfig.getCryptocompare().getKey())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::mapToCryptocurrencyList)
                .onErrorResume(e -> {
                    log.warn("Error fetching data from CryptoCompare: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Cryptocurrency mapToCryptocurrency(Map<String, Object> data) {
        if (data == null || !data.containsKey("RAW") || data.get("FROMSYMBOL") == null) {
            log.warn("Invalid data format: {}", data);
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rawData = (Map<String, Object>) data.get("RAW");
        @SuppressWarnings("unchecked")
        Map<String, Object> symbolData = (Map<String, Object>) rawData.get(data.get("FROMSYMBOL"));
        if (symbolData == null || !symbolData.containsKey("USD")) {
            log.warn("Invalid RAW data format: {}", symbolData);
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> usdData = (Map<String, Object>) symbolData.get("USD");
        return Cryptocurrency.builder()
                .id(safeString(data.get("FROMSYMBOL")).toLowerCase())
                .name(safeString(data.get("NAME")))
                .symbol(safeString(data.get("FROMSYMBOL")))
                .price(safeBigDecimal(usdData.get("PRICE")))
                .marketCap(safeBigDecimal(usdData.get("MKTCAP")))
                .volume24h(safeBigDecimal(usdData.get("VOLUME24HOUR")))
                .percentChange24h(safeBigDecimal(usdData.get("CHANGEPCT24HOUR")))
                .image("https://www.cryptocompare.com" + safeString(data.get("IMAGEURL")))
                .rank(safeInteger(data.get("SORTORDER")))
                .circulatingSupply(safeBigDecimal(usdData.get("SUPPLY")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Cryptocurrency> mapToCryptocurrencyList(Map<String, Object> response) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("Data");
        if (data == null) {
            log.warn("Invalid data format: {}", response);
            return Collections.emptyList();
        }
        return data.stream()
                .filter(item -> item.get("CoinInfo") != null && item.get("RAW") != null)
                .map(item -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> coinInfo = (Map<String, Object>) item.get("CoinInfo");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> raw = (Map<String, Object>) ((Map<String, Object>) item.get("RAW")).get("USD");
                    return Cryptocurrency.builder()
                            .id(safeString(coinInfo.get("Name")).toLowerCase())
                            .name(safeString(coinInfo.get("FullName")))
                            .symbol(safeString(coinInfo.get("Name")))
                            .price(safeBigDecimal(raw.get("PRICE")))
                            .marketCap(safeBigDecimal(raw.get("MKTCAP")))
                            .volume24h(safeBigDecimal(raw.get("VOLUME24HOUR")))
                            .percentChange24h(safeBigDecimal(raw.get("CHANGEPCT24HOUR")))
                            .image("https://www.cryptocompare.com" + safeString(coinInfo.get("ImageUrl")))
                            .rank(safeInteger(coinInfo.get("SortOrder")))
                            .circulatingSupply(safeBigDecimal(raw.get("SUPPLY")))
                            .totalSupply(safeBigDecimal(raw.get("TOTALSUPPLY")))
                            .maxSupply(safeBigDecimal(raw.get("MAXSUPPLY")))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> mapToNews(Map<String, Object> response) {
        List<Map<String, Object>> newsData = (List<Map<String, Object>>) response.get("Data");
        if (newsData == null) {
            log.warn("Invalid news data format: {}", response);
            return Collections.emptyList();
        }
        return newsData.stream()
                .map(news -> Map.of(
                        "title", safeString(news.get("title")),
                        "url", safeString(news.get("url")),
                        "source", safeString(news.get("source")),
                        "published_at", safeString(news.get("published_on"))
                ))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapToTeamData(Map<String, Object> response) {
        if (response == null) {
            log.warn("Invalid team data format: {}", response);
            return Collections.emptyMap();
        }
        List<Map<String, Object>> team = (List<Map<String, Object>>) response.get("team");
        if (team == null) {
            log.warn("No team data available: {}", response);
            return Collections.emptyMap();
        }
        return Map.of(
                "team", team.stream()
                        .map(member -> Map.of(
                                "name", safeString(member.get("name")),
                                "position", safeString(member.get("position")),
                                "description", safeString(member.get("description"))
                        ))
                        .collect(Collectors.toList()),
                "description", safeString(response.get("description")),
                "website", safeListString(response.get("links")).isEmpty() ? "N/A" :
                        safeListString(response.get("links")).get(0),
                "social_links", response.getOrDefault("social", Collections.emptyMap())
        );
    }

    @SuppressWarnings("unchecked")
    private List<List<Number>> mapToMarketChart(Map<String, Object> response) {
        Map<String, Object> data = (Map<String, Object>) response.get("Data");
        if (data == null) {
            log.warn("Invalid market chart data format: {}", response);
            return Collections.emptyList();
        }
        List<Map<String, Object>> prices = (List<Map<String, Object>>) data.get("Data");
        if (prices == null) {
            log.warn("No price data available: {}", response);
            return Collections.emptyList();
        }
        return prices.stream()
                .map(price -> Arrays.asList(
                        (Number) (safeLong(price.get("time")) * 1000),
                        (Number) safeDouble(price.get("close"))
                ))
                .collect(Collectors.toList());
    }

    private Mono<List<Cryptocurrency>> getCoinGeckoData(int page, int perPage) {
        return webClient.get()
                .uri(apiConfig.getCoingecko().getBaseUrl() + "/coins/markets?vs_currency=usd&page={page}&per_page={perPage}",
                        page, perPage)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .map(this::mapFromCoinGeckoData)
                .onErrorResume(e -> {
                    log.warn("Error fetching data from CoinGecko: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private List<Cryptocurrency> mapFromCoinGeckoData(List<Map<String, Object>> data) {
        if (data == null) {
            log.warn("Invalid CoinGecko data format: null");
            return Collections.emptyList();
        }
        return data.stream()
                .map(item -> Cryptocurrency.builder()
                        .id(safeString(item.get("id")))
                        .name(safeString(item.get("name")))
                        .symbol(safeString(item.get("symbol")))
                        .price(safeBigDecimal(item.get("current_price")))
                        .marketCap(safeBigDecimal(item.get("market_cap")))
                        .volume24h(safeBigDecimal(item.get("total_volume")))
                        .percentChange24h(safeBigDecimal(item.get("price_change_percentage_24h")))
                        .image(safeString(item.get("image")))
                        .rank(safeInteger(item.get("market_cap_rank")))
                        .circulatingSupply(safeBigDecimal(item.get("circulating_supply")))
                        .totalSupply(safeBigDecimal(item.get("total_supply")))
                        .maxSupply(safeBigDecimal(item.get("max_supply")))
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Mono<List<Cryptocurrency>> getMobulaData(int page, int perPage) {
        return webClient.get()
                .uri(apiConfig.getMobula().getBaseUrl() + "/market/multi?limit={limit}&page={page}",
                        perPage, page)
                .headers(headers -> headers.add("Authorization", "Bearer " + apiConfig.getMobula().getKey()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> mapFromMobulaData((List<Map<String, Object>>)
                        response.getOrDefault("data", Collections.emptyList())))
                .onErrorResume(e -> {
                    log.warn("Error fetching data from Mobula: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private List<Cryptocurrency> mapFromMobulaData(List<Map<String, Object>> data) {
        if (data == null) {
            log.warn("Invalid Mobula data format: null");
            return Collections.emptyList();
        }
        return data.stream()
                .map(item -> Cryptocurrency.builder()
                        .id(safeString(item.get("id")).toLowerCase())
                        .name(safeString(item.get("name")))
                        .symbol(safeString(item.get("symbol")))
                        .price(safeBigDecimal(item.get("price")))
                        .marketCap(safeBigDecimal(item.get("market_cap")))
                        .volume24h(safeBigDecimal(item.get("volume_24h")))
                        .percentChange24h(safeBigDecimal(item.get("price_change_24h")))
                        .image(safeString(item.get("logo")))
                        .rank(safeInteger(item.get("rank")))
                        .circulatingSupply(safeBigDecimal(item.get("circulating_supply")))
                        .totalSupply(safeBigDecimal(item.get("total_supply")))
                        .maxSupply(safeBigDecimal(item.get("max_supply")))
                        .build())
                .collect(Collectors.toList());
    }

    private Mono<List<Map<String, String>>> getCryptoCompareNews(String symbol) {
        return webClient.get()
                .uri(apiConfig.getCryptocompare().getBaseUrl() + "/v2/news/?categories=" + symbol)
                .header("Authorization", "Apikey " + apiConfig.getCryptocompare().getKey())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::mapToNews)
                .onErrorResume(e -> {
                    log.warn("Error fetching news from CryptoCompare: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<List<Map<String, String>>> getCoinGeckoNews(String id) {
        return Mono.just(Collections.emptyList());
    }

    private String safeString(Object obj) {
        return obj != null ? obj.toString() : "N/A";
    }

    private Integer safeInteger(Object obj) {
        if (obj == null) return 0;
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer: {}", obj);
            return 0;
        }
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

    private Long safeLong(Object obj) {
        if (obj == null) return 0L;
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long: {}", obj);
            return 0L;
        }
    }

    private Double safeDouble(Object obj) {
        if (obj == null) return 0.0;
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Double: {}", obj);
            return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> safeListString(Object obj) {
        if (obj == null) return Collections.emptyList();
        try {
            return (List<String>) obj;
        } catch (ClassCastException e) {
            log.warn("Failed to cast to List<String>: {}", obj);
            return Collections.emptyList();
        }
    }

    private Mono<Map<String, Object>> getDetailedData(String id) {
        return webClient.get()
                .uri(apiConfig.getCoingecko().getBaseUrl() + "/coins/" + id +
                        "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnError(e -> log.warn("Error fetching detailed data for id {}: {}", id, e.getMessage()))
                .onErrorReturn(Collections.emptyMap());
    }

    private Mono<Map<String, Object>> getFallbackDetailedData(String id) {
        return webClient.get()
                .uri(apiConfig.getMobula().getBaseUrl() + "/tokens/" + id)
                .headers(headers -> headers.add("Authorization", "Bearer " + apiConfig.getMobula().getKey()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnError(e -> log.warn("Error fetching fallback data for id {}: {}", id, e.getMessage()))
                .onErrorReturn(Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    private Mono<List<List<Number>>> getMarketChartFallback(String symbol, int days) {
        return webClient.get()
                .uri(apiConfig.getCoingecko().getBaseUrl() +
                        "/coins/{id}/market_chart?vs_currency=usd&days={days}", symbol, days)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> {
                    List<List<Object>> prices = (List<List<Object>>) response.get("prices");
                    if (prices == null) return Collections.<List<Number>>emptyList();
                    return prices.stream()
                            .map(price -> Arrays.<Number>asList(
                                    safeLong(price.get(0)),
                                    safeDouble(price.get(1))
                            ))
                            .collect(Collectors.toList());
                })
                .onErrorResume(e -> {
                    log.warn("Error fetching market chart fallback for symbol {}: {}", symbol, e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    private <T> Mono<T> handleError(Throwable error, String operation) {
        log.error("Error during {}: {}", operation, error.getMessage(), error);
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcError = (WebClientResponseException) error;
            throw new ApiException(
                    wcError.getMessage(),
                    "API",
                    wcError.getStatusCode().value()
            );
        }
        return Mono.error(new ApiException(error.getMessage(), "API", 500));
    }
}








// package crypto.insight.crypto.service;

// import crypto.insight.crypto.config.ApiConfig;
// import crypto.insight.crypto.model.Cryptocurrency;
// import crypto.insight.crypto.exception.ApiException;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.core.ParameterizedTypeReference;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.web.reactive.function.client.WebClientResponseException;
// import reactor.core.publisher.Mono;

// import java.math.BigDecimal;
// import java.util.*;
// import java.util.stream.Collectors;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class ApiService {
//     private final WebClient webClient;
//     private final ApiConfig apiConfig;

//     @Cacheable(value = "cryptoData", key = "#symbol")
//     public Mono<Cryptocurrency> getCryptocurrencyData(String symbol) {
//         return webClient.get()
//                 .uri(apiConfig.getCryptocompare().getBaseUrl() + "/pricemultifull?fsyms=" + symbol + "&tsyms=USD")
//                 .header("Authorization", "Apikey " + apiConfig.getCryptocompare().getKey())
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(this::mapToCryptocurrency)
//                 .onErrorResume(e -> {
//                     log.warn("Error fetching data from CryptoCompare for symbol {}: {}", symbol, e.getMessage());
//                     return getFallbackData(symbol);
//                 });
//     }

//     private Mono<Cryptocurrency> getFallbackData(String symbol) {
//         return webClient.get()
//                 .uri(apiConfig.getCoingecko().getBaseUrl() + "/simple/price?ids=" + symbol + "&vs_currencies=usd")
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(this::mapToCryptocurrency)
//                 .onErrorResume(e -> {
//                     log.error("Error fetching fallback data for symbol {}: {}", symbol, e.getMessage());
//                     return Mono.just(null);
//                 });
//     }

//     @Cacheable(value = "marketChart", key = "#symbol + #days")
//     public Mono<List<List<Number>>> getMarketChart(String symbol, int days) {
//         return webClient.get()
//                 .uri(apiConfig.getCryptocompare().getBaseUrl() + "/v2/histoday?fsym=" + symbol + "&tsym=USD&limit=" + days)
//                 .header("Authorization", "Apikey " + apiConfig.getCryptocompare().getKey())
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(this::mapToMarketChart)
//                 .onErrorResume(e -> {
//                     log.warn("Error fetching market chart from CryptoCompare for symbol {}: {}", symbol, e.getMessage());
//                     return getMarketChartFallback(symbol, days);
//                 });
//     }

//     @Cacheable(value = "cryptoList")
//     public Mono<List<Cryptocurrency>> getCryptocurrencies(int page, int perPage) {
//         return getCryptoCompareData(page, perPage)
//                 .switchIfEmpty(getCoinGeckoData(page, perPage))
//                // .switchIfEmpty(getMobulaData(page, perPage))
//                 .onErrorResume(e -> {
//                     log.error("Error fetching cryptocurrency list: {}", e.getMessage(), e);
//                     return Mono.just(Collections.emptyList());
//                 });
//     }

//     @Cacheable(value = "cryptoDetails", key = "#id")
//     public Mono<Map> getCryptocurrencyDetails(String id) {
//         return getDetailedData(id)
//                 .switchIfEmpty(getFallbackDetailedData(id))
//                 .onErrorResume(e -> {
//                     log.error("Error fetching cryptocurrency details for id {}: {}", id, e.getMessage(), e);
//                     return Mono.just(Collections.emptyMap());
//                 });
//     }

//     @Cacheable(value = "teamData", key = "#id")
//     public Mono<Map> getTeamData(String id) {
//         return webClient.get()
//                 .uri(apiConfig.getCoinpaprika().getBaseUrl() + "/coins/" + id + "/teams")
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .onErrorResume(e -> {
//                     log.warn("Error fetching team data for id {}: {}", id, e.getMessage());
//                     return Mono.just(Collections.emptyMap());
//                 });
//     }

//     @Cacheable(value = "cryptoNews", key = "#symbol")
//     public Mono<List<Map<String, String>>> getCombinedCryptoData(String symbol, String id) {
//         return getCryptoCompareNews(symbol)
//                 .switchIfEmpty(getCoinGeckoNews(id))
//                 .onErrorResume(e -> {
//                     log.error("Error fetching news data for symbol {}: {}", symbol, e.getMessage(), e);
//                     return Mono.just(Collections.emptyList());
//                 });
//     }

//     private Mono<List<Cryptocurrency>> getCryptoCompareData(int page, int perPage) {
//         return webClient.get()
//                 .uri(apiConfig.getCryptocompare().getBaseUrl() + "/top/mktcapfull?limit={limit}&tsym=USD&page={page}",
//                         perPage, page - 1)
//                 .header("Authorization", "Apikey " + apiConfig.getCryptocompare().getKey())
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(this::mapToCryptocurrencyList)
//                 .onErrorResume(e -> {
//                     log.warn("Error fetching data from CryptoCompare: {}", e.getMessage());
//                     return Mono.empty();
//                 });
//     }

//     private Cryptocurrency mapToCryptocurrency(Map<String, Object> data) {
//         if (data == null || !data.containsKey("RAW") || data.get("FROMSYMBOL") == null) {
//             log.warn("Invalid data format: {}", data);
//             return null;
//         }
//         Map<String, Object> rawData = (Map<String, Object>) data.get("RAW");
//         Map<String, Object> symbolData = (Map<String, Object>) rawData.get(data.get("FROMSYMBOL"));
//         if (symbolData == null || !symbolData.containsKey("USD")) {
//             log.warn("Invalid RAW data format: {}", symbolData);
//             return null;
//         }
//         Map<String, Object> usdData = (Map<String, Object>) symbolData.get("USD");
//         return Cryptocurrency.builder()
//                 .id(safeString(data.get("FROMSYMBOL")).toLowerCase())
//                 .name(safeString(data.get("NAME")))
//                 .symbol(safeString(data.get("FROMSYMBOL")))
//                 .price(safeBigDecimal(usdData.get("PRICE")))
//                 .marketCap(safeBigDecimal(usdData.get("MKTCAP")))
//                 .volume24h(safeBigDecimal(usdData.get("VOLUME24HOUR")))
//                 .percentChange24h(safeBigDecimal(usdData.get("CHANGEPCT24HOUR")))
//                 .image("https://www.cryptocompare.com" + safeString(data.get("IMAGEURL")))
//                 .rank(safeInteger(data.get("SORTORDER")))
//                 .circulatingSupply(safeBigDecimal(usdData.get("SUPPLY")))
//                 .build();
//     }

//     private List<Cryptocurrency> mapToCryptocurrencyList(Map<String, Object> response) {
//         List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("Data");
//         if (data == null) {
//             log.warn("Invalid data format: {}", response);
//             return Collections.emptyList();
//         }
//         return data.stream()
//                 .filter(item -> item.get("CoinInfo") != null && item.get("RAW") != null)
//                 .map(item -> {
//                     Map<String, Object> coinInfo = (Map<String, Object>) item.get("CoinInfo");
//                     Map<String, Object> raw = (Map<String, Object>) ((Map<String, Object>) item.get("RAW")).get("USD");
//                     return Cryptocurrency.builder()
//                             .id(safeString(coinInfo.get("Name")).toLowerCase())
//                             .name(safeString(coinInfo.get("FullName")))
//                             .symbol(safeString(coinInfo.get("Name")))
//                             .price(safeBigDecimal(raw.get("PRICE")))
//                             .marketCap(safeBigDecimal(raw.get("MKTCAP")))
//                             .volume24h(safeBigDecimal(raw.get("VOLUME24HOUR")))
//                             .percentChange24h(safeBigDecimal(raw.get("CHANGEPCT24HOUR")))
//                             .image("https://www.cryptocompare.com" + safeString(coinInfo.get("ImageUrl")))
//                             .rank(safeInteger(coinInfo.get("SortOrder")))
//                             .circulatingSupply(safeBigDecimal(raw.get("SUPPLY")))
//                             .totalSupply(safeBigDecimal(raw.get("TOTALSUPPLY")))
//                             .maxSupply(safeBigDecimal(raw.get("MAXSUPPLY")))
//                             .build();
//                 })
//                 .collect(Collectors.toList());
//     }

//     private List<Map<String, String>> mapToNews(Map<String, Object> response) {
//         List<Map<String, Object>> newsData = (List<Map<String, Object>>) response.get("Data");
//         if (newsData == null) {
//             log.warn("Invalid news data format: {}", response);
//             return Collections.emptyList();
//         }
//         return newsData.stream()
//                 .map(news -> Map.of(
//                         "title", safeString(news.get("title")),
//                         "url", safeString(news.get("url")),
//                         "source", safeString(news.get("source")),
//                         "published_at", safeString(news.get("published_on"))
//                 ))
//                 .collect(Collectors.toList());
//     }

//     private Map<String, Object> mapToTeamData(Map<String, Object> response) {
//         if (response == null) {
//             log.warn("Invalid team data format: {}", response);
//             return Collections.emptyMap();
//         }
//         List<Map<String, Object>> team = (List<Map<String, Object>>) response.get("team");
//         if (team == null) {
//             log.warn("No team data available: {}", response);
//             return Collections.emptyMap();
//         }
//         return Map.of(
//                 "team", team.stream()
//                         .map(member -> Map.of(
//                                 "name", safeString(member.get("name")),
//                                 "position", safeString(member.get("position")),
//                                 "description", safeString(member.get("description"))
//                         ))
//                         .collect(Collectors.toList()),
//                 "description", safeString(response.get("description")),
//                 "website", safeListString(response.get("links")).isEmpty() ? "N/A" :
//                         safeListString(response.get("links")).get(0),
//                 "social_links", response.getOrDefault("social", Collections.emptyMap())
//         );
//     }

//     private List<List<Number>> mapToMarketChart(Map<String, Object> response) {
//         Map<String, Object> data = (Map<String, Object>) response.get("Data");
//         if (data == null) {
//             log.warn("Invalid market chart data format: {}", response);
//             return Collections.emptyList();
//         }
//         List<Map<String, Object>> prices = (List<Map<String, Object>>) data.get("Data");
//         if (prices == null) {
//             log.warn("No price data available: {}", response);
//             return Collections.emptyList();
//         }
//         return prices.stream()
//                 .map(price -> Arrays.asList(
//                         (Number) (safeLong(price.get("time")) * 1000),
//                         (Number) safeDouble(price.get("close"))
//                 ))
//                 .collect(Collectors.toList());
//     }

//     private Mono<List<Cryptocurrency>> getCoinGeckoData(int page, int perPage) {
//         return webClient.get()
//                 .uri(apiConfig.getCoingecko().getBaseUrl() + "/coins/markets?vs_currency=usd&page={page}&per_page={perPage}",
//                         page, perPage)
//                 .retrieve()
//                 .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
//                 .map(this::mapFromCoinGeckoData)
//                 .onErrorResume(e -> {
//                     log.warn("Error fetching data from CoinGecko: {}", e.getMessage());
//                     return Mono.empty();
//                 });
//     }

//     private List<Cryptocurrency> mapFromCoinGeckoData(List<Map<String, Object>> data) {
//         if (data == null) {
//             log.warn("Invalid CoinGecko data format: null");
//             return Collections.emptyList();
//         }
//         return data.stream()
//                 .map(item -> Cryptocurrency.builder()
//                         .id(safeString(item.get("id")))
//                         .name(safeString(item.get("name")))
//                         .symbol(safeString(item.get("symbol")))
//                         .price(safeBigDecimal(item.get("current_price")))
//                         .marketCap(safeBigDecimal(item.get("market_cap")))
//                         .volume24h(safeBigDecimal(item.get("total_volume")))
//                         .percentChange24h(safeBigDecimal(item.get("price_change_percentage_24h")))
//                         .image(safeString(item.get("image")))
//                         .rank(safeInteger(item.get("market_cap_rank")))
//                         .circulatingSupply(safeBigDecimal(item.get("circulating_supply")))
//                         .totalSupply(safeBigDecimal(item.get("total_supply")))
//                         .maxSupply(safeBigDecimal(item.get("max_supply")))
//                         .build())
//                 .collect(Collectors.toList());
//     }

//     private Mono<List<Cryptocurrency>> getMobulaData(String page, String perPage) {
//         return webClient.get()
//                 .uri(apiConfig.getMobula().getBaseUrl() + "/market/multi?limit={limit}&page={page}",
//                         perPage, page)
//                 .headers(headers -> headers.add("Authorization", "Bearer " + apiConfig.getMobula().getKey()))
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(response -> mapFromMobulaData((List<Map<String, Object>>)
//                         response.getOrDefault("data", Collections.emptyList())))
//                 .onErrorResume(e -> {
//                     log.warn("Error fetching data from Mobula: {}", e.getMessage());
//                     return Mono.empty();
//                 });
//     }

//     private List<Cryptocurrency> mapFromMobulaData(List<Map<String, Object>> data) {
//         if (data == null) {
//             log.warn("Invalid Mobula data format: null");
//             return Collections.emptyList();
//         }
//         return data.stream()
//                 .map(item -> Cryptocurrency.builder()
//                         .id(safeString(item.get("id")).toLowerCase())
//                         .name(safeString(item.get("name")))
//                         .symbol(safeString(item.get("symbol")))
//                         .price(safeBigDecimal(item.get("price")))
//                         .marketCap(safeBigDecimal(item.get("market_cap")))
//                         .volume24h(safeBigDecimal(item.get("volume_24h")))
//                         .percentChange24h(safeBigDecimal(item.get("price_change_24h")))
//                         .image(safeString(item.get("logo")))
//                         .rank(safeInteger(item.get("rank")))
//                         .circulatingSupply(safeBigDecimal(item.get("circulating_supply")))
//                         .totalSupply(safeBigDecimal(item.get("total_supply")))
//                         .maxSupply(safeBigDecimal(item.get("max_supply")))
//                         .build())
//                 .collect(Collectors.toList());
//     }

//     private Mono<List<Map<String, String>>> getCryptoCompareNews(String symbol) {
//         return webClient.get()
//                 .uri(apiConfig.getCryptocompare().getBaseUrl() + "/v2/news/?categories=" + symbol)
//                 .header("Authorization", "Apikey " + apiConfig.getCryptocompare().getKey())
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(this::mapToNews)
//                 .onErrorResume(e -> {
//                     log.warn("Error fetching news from CryptoCompare: {}", e.getMessage());
//                     return Mono.empty();
//                 });
//     }

//     private Mono<List<Map<String, String>>> getCoinGeckoNews(String id) {
//         return Mono.just(Collections.emptyList());
//     }

//     private String safeString(Object obj) {
//         return obj != null ? obj.toString() : "N/A";
//     }

//     private Integer safeInteger(Object obj) {
//         if (obj == null) return 0;
//         try {
//             return Integer.parseInt(obj.toString());
//         } catch (NumberFormatException e) {
//             log.warn("Failed to parse integer: {}", obj);
//             return 0;
//         }
//     }

//     private BigDecimal safeBigDecimal(Object obj) {
//         if (obj == null) return BigDecimal.ZERO;
//         try {
//             return new BigDecimal(obj.toString());
//         } catch (NumberFormatException e) {
//             log.warn("Failed to parse BigDecimal: {}", obj);
//             return BigDecimal.ZERO;
//         }
//     }

//     private Long safeLong(Object obj) {
//         if (obj == null) return 0L;
//         try {
//             return Long.parseLong(obj.toString());
//         } catch (NumberFormatException e) {
//             log.warn("Failed to parse Long: {}", obj);
//             return 0L;
//         }
//     }

//     private Double safeDouble(Object obj) {
//         if (obj == null) return 0.0;
//         try {
//             return Double.parseDouble(obj.toString());
//         } catch (NumberFormatException e) {
//             log.warn("Failed to parse Double: {}", obj);
//             return 0.0;
//         }
//     }

//     private List<String> safeListString(Object obj) {
//         if (obj == null) return Collections.emptyList();
//         try {
//             return (List<String>) obj;
//         } catch (ClassCastException e) {
//             log.warn("Failed to cast to List<String>: {}", obj);
//             return Collections.emptyList();
//         }
//     }

//     private Mono<Map> getDetailedData(String id) {
//         return webClient.get()
//                 .uri(apiConfig.getCoingecko().getBaseUrl() + "/coins/" + id +
//                         "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false")
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .doOnError(e -> log.warn("Error fetching detailed data for id {}: {}", id, e.getMessage()))
//                 .onErrorReturn(Collections.emptyMap());
//     }

//     private Mono<Map> getFallbackDetailedData(String id) {
//         return webClient.get()
//                 .uri(apiConfig.getMobula().getBaseUrl() + "/tokens/" + id)
//                 .headers(headers -> headers.add("Authorization", "Bearer " + apiConfig.getMobula().getKey()))
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .doOnError(e -> log.warn("Error fetching fallback data for id {}: {}", id, e.getMessage()))
//                 .onErrorReturn(Collections.emptyMap());
//     }

//     private Mono<List<List<Number>>> getMarketChartFallback(String symbol, int days) {
//         return webClient.get()
//                 .uri(apiConfig.getCoingecko().getBaseUrl() +
//                         "/coins/{id}/market_chart?vs_currency=usd&days={days}", symbol, days)
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .map(response -> {
//                     List<List<Object>> prices = (List<List<Object>>) response.get("prices");
//                     if (prices == null) return Collections.<List<Number>>emptyList();
//                     return prices.stream()
//                             .map(price -> Arrays.<Number>asList(
//                                     safeLong(price.get(0)),
//                                     safeDouble(price.get(1))
//                             ))
//                             .collect(Collectors.toList());
//                 })
//                 .onErrorResume(e -> {
//                     log.warn("Error fetching market chart fallback for symbol {}: {}", symbol, e.getMessage());
//                     return Mono.just(Collections.emptyList());
//                 });
//     }

//     private <T> Mono<T> handleError(Throwable error, String operation) {
//         log.error("Error during {}: {}", operation, error.getMessage(), error);
//         if (error instanceof WebClientResponseException) {
//             WebClientResponseException wcError = (WebClientResponseException) error;
//             throw new ApiException(
//                     wcError.getMessage(),
//                     "API",
//                     wcError.getStatusCode().value()
//             );
//         }
//         return Mono.error(new ApiException(error.getMessage(), "API", 500));
//     }
// }