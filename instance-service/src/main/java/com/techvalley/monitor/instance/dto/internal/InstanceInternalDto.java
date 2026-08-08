package com.techvalley.monitor.instance.dto.internal;

import com.techvalley.monitor.instance.Instance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceInternalDto {
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

    public static InstanceInternalDto from(Instance i) {
        return InstanceInternalDto.builder()
                .id(i.getId())
                .instanceName(i.getInstanceName())
                .region(i.getRegion())
                .instanceType(i.getInstanceType() != null ? i.getInstanceType().name() : null)
                .clientId(i.getClientId())
                .status(i.getStatus() != null ? i.getStatus().name() : null)
                .cpuUsage(i.getCpuUsage())
                .monthlyCost(i.getMonthlyCost())
                .launcheAt(i.getLauncheAt())
                .updateAt(i.getUpdateAt())
                .build();
    }
}