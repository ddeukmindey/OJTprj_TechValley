package com.techvalley.monitor.client.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientCostForecastResponse {
    private Long clientId;
    private Double currentSpent;
    private Double projectedSpentEndOfMonth;
    private Integer activeRunningInstances;
    private String recommendation;
}
