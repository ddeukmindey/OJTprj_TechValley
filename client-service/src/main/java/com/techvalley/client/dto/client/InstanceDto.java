package com.techvalley.client.dto.client;

import com.techvalley.client.enums.InstanceStatus;
import com.techvalley.client.enums.InstanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceDto {
    private Long id;
    private Long clientId;
    private String instanceName;
    private InstanceType instanceType;
    private String region;
    private InstanceStatus status;
    private Float cpuUsage;
    private Float monthlyCost;
    private LocalDateTime launcheAt;
    private LocalDateTime updateAt;
}
