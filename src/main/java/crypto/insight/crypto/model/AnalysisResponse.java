package crypto.insight.crypto.model;

import lombok.Builder;
import lombok.Value;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class AnalysisResponse {
    Map<String, String> analysis;
    CryptoDetails details;
    List<ChartDataPoint> chartData;
    Map<String, Object> teamData;
    List<Map<String, String>> newsData;
}