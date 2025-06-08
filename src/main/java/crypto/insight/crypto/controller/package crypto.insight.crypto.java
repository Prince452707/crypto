// package crypto.insight.crypto.controller;

// import crypto.insight.crypto.model.*;
// import crypto.insight.crypto.service.*;
// import lombok.RequiredArgsConstructor;
// import org.springframework.web.bind.annotation.*;
// import reactor.core.publisher.Mono;
// import java.util.List;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/v1/crypto")
// @RequiredArgsConstructor
// public class CryptoController {
//     private final ApiService apiService;
//     private final AIService aiService;

//     @GetMapping("/analysis/{symbol}")
//     public Mono<Map<String, String>> getAnalysis(
//             @PathVariable String symbol,
//             @RequestParam(defaultValue = "30") int days) {
//         return apiService.getCryptocurrencyData(symbol)
//                 .flatMap(crypto -> 
//                     Mono.zip(
//                         apiService.getCryptocurrencyDetails(crypto.getId()),
//                         apiService.getMobulaData(crypto.getId(), crypto.getSymbol()),
//                         apiService.getMarketChart(symbol, days),
//                         apiService.getCombinedCryptoData(symbol, crypto.getId())
//                     ).flatMap(tuple -> {
//                         String context = aiService.buildAnalysisContext(
//                             crypto, tuple.getT1(), tuple.getT2(), 
//                             tuple.getT3(), tuple.getT4(), days
//                         );
//                         return Mono.fromFuture(aiService.generateComprehensiveAnalysis(context));
//                     })
//                 );
//     }

//     @GetMapping("/market-data")
//     public Mono<Mono<List<Cryptocurrency>>> getMarketData(
//             @RequestParam(defaultValue = "1") int page,
//             @RequestParam(defaultValue = "100") int perPage) {
//         return Mono.fromCallable(() -> apiService.getCryptocurrencies(page, perPage));
//     }

//     @GetMapping("/details/{id}")
//     public Mono<Mono<Map>> getCryptoDetails(@PathVariable String id) {
//         return Mono.fromCallable(() -> apiService.getCryptocurrencyDetails(id));
//     }

//     @GetMapping("/chart/{symbol}")
//     public Mono<List<ChartDataPoint>> getPriceChart(
//             @PathVariable String symbol,
//             @RequestParam(defaultValue = "30") int days) {
//         return apiService.getMarketChart(symbol, days)
//                 .map(data -> data.stream()
//                     .map(point -> new ChartDataPoint(
//                     ))
//                     .toList()
//                 );
//     }
// }
