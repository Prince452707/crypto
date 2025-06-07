package crypto.model;

import lombok.Value;
import java.util.List;
import java.util.Map;

@Value
public class AnalysisResponse {
    Map<String, String> analysis;
    CryptoDetails details;
    List<ChartDataPoint> chartData;
    Map<String, Object> teamData;
    List<Map<String, String>> newsData;
}
