package com.techvalley.monitor.monitoring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringReportResponse {

    private long totalInstances;
    private long runningInstances;
    private long stoppedInstances;
    private long errorInstances;
    private double averageCpuUsage;
    private long unresolvedAlerts;
    private long totalClients;
}
