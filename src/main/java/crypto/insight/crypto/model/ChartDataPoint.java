package crypto.insight.crypto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPoint {
    private Long timestamp;
    private Double price;
}
