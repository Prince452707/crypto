
package crypto.insight.crypto.controller;

import crypto.insight.crypto.model.*;
import crypto.insight.crypto.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/crypto")
@RequiredArgsConstructor
public class CryptoController {
    private final ApiService apiService;
    private final AIService aiService;

    @GetMapping("/analysis/{symbol}")
    public Mono<ResponseEntity<? extends Object>> getAnalysis(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "30") int days) {
        return apiService.getCryptocurrencyData(symbol)
                .flatMap(crypto -> {
                    if (crypto == null) {
                        return Mono.just(ResponseEntity
                                .<ApiResponse<AnalysisResponse>>notFound()
                                .build());
                    }
                    return Mono.zip(
                            apiService.getCryptocurrencyDetails(crypto.getId()),
                            apiService.getTeamData(crypto.getId()),
                            apiService.getMarketChart(symbol, days),
                            apiService.getCombinedCryptoData(crypto.getSymbol(), crypto.getId())
                    ).flatMap(tuple -> {
                        List<List<Number>> marketChart = tuple.getT3();
                        List<ChartDataPoint> chartDataPoints = marketChart != null ?
                                marketChart.stream()
                                        .map(point -> new ChartDataPoint(
                                                ((Number) point.get(0)).longValue(),
                                                ((Number) point.get(1)).doubleValue()
                                        ))
                                        .collect(Collectors.toList()) :
                                List.of();
                        return Mono.fromFuture(aiService.generateComprehensiveAnalysis(
                                crypto,
                                chartDataPoints,
                                days
                        )).map(analysis -> ResponseEntity.ok(ApiResponse.success(
                                analysis,
                                "Analysis fetched successfully"
                        )));
                    });
                })
                .onErrorResume(e -> {
                    log.error("Error fetching analysis for symbol {}: {}", symbol, e.getMessage(), e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(ApiResponse.error("Error fetching analysis: " + e.getMessage())));
                });
    }

    @GetMapping("/market-data")
    public Mono<ResponseEntity<ApiResponse<List<Cryptocurrency>>>> getMarketData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int perPage) {
        return apiService.getCryptocurrencies(page, perPage)
                .map(data -> ResponseEntity.ok(ApiResponse.success(data, "Market data fetched successfully")))
                .onErrorResume(e -> {
                    log.error("Error fetching market data: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(ApiResponse.error("Error fetching market data: " + e.getMessage())));
                });
    }

    @GetMapping("/details/{id}")
    public Mono<ResponseEntity<ApiResponse<CryptoDetails>>> getCryptoDetails(@PathVariable String id) {
        return apiService.getCryptocurrencyDetails(id)
                .map(details -> ResponseEntity.ok(ApiResponse.success(
                        new CryptoDetails(details),
                        "Crypto details fetched successfully"
                )))
                .onErrorResume(e -> {
                    log.error("Error fetching crypto details for id {}: {}", id, e.getMessage(), e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(ApiResponse.error("Error fetching details: " + e.getMessage())));
                });
    }

    @GetMapping("/chart/{symbol}")
    public Mono<ResponseEntity<ApiResponse<List<ChartDataPoint>>>> getPriceChart(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "30") int days) {
        return apiService.getMarketChart(symbol, days)
                .map(data -> {
                    List<ChartDataPoint> chartData = data.stream()
                            .map(point -> new ChartDataPoint(
                                    ((Number) point.get(0)).longValue(),
                                    ((Number) point.get(1)).doubleValue()
                            ))
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(ApiResponse.success(
                            chartData,
                            "Chart data fetched successfully"
                    ));
                })
                .onErrorResume(e -> {
                    log.error("Error fetching chart data for symbol {}: {}", symbol, e.getMessage(), e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(ApiResponse.error("Error fetching chart data: " + e.getMessage())));
                });
    }
}