package com.techvalley.monitor.monitoring.dto.client;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InstanceDto {
    private Long id;
    private String instanceName;
    private String region;
    private String instanceType;
    private Long clientId;
    private String status;
    private Float cpuUsage;
    private Float monthlyCost;
    private LocalDateTime launcheAt;
    private LocalDateTime updateAt;
}