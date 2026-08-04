package com.techvalley.monitor.client.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSlaResponse {
    private Long clientId;
    private String month;
    private Double slaPercentage;
    private Double targetSla;
    private String status;
    private Double totalDowntimeHours;
}
