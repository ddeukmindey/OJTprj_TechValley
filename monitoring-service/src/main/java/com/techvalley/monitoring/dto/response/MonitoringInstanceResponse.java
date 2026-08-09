package com.techvalley.monitoring.dto.response;

import com.techvalley.monitoring.enums.InstanceStatus;
import com.techvalley.monitoring.enums.InstanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringInstanceResponse {

    private Long id;
    private String instanceName;
    private String region;
    private InstanceType instanceType;
    private InstanceStatus status;
    private Float cpuUsage;
    private Float monthlyCost;
    private Long clientId;
    private LocalDateTime launcheAt;
    private LocalDateTime updateAt;
    private String warningMessage;
}
