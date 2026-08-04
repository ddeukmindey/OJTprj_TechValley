package com.techvalley.monitor.instance.dto.response;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.enums.InstanceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InstanceResponse {
    private Long id;
    private Long clientId;
    private String name;
    private String region;
    private InstanceType type;
    private InstanceStatus status;
    private Float cpuUsage;
    private Float monthlyCost;
    private LocalDateTime launcheAt;
    private LocalDateTime updateAt;
}
