package com.techvalley.monitor.client.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientCostResponse {
    private Long clientId;
    private String currentMonth;
    private Integer totalInstances;
    private Double runningCost;
    private Double stoppedCost;
    private Double totalMonthlyCost;
}
