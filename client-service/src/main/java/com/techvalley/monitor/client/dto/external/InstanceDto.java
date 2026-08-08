package com.techvalley.monitor.client.dto.external;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.enums.InstanceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InstanceDto {
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
}