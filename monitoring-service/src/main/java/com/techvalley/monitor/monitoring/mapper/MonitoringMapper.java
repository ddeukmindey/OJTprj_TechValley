package com.techvalley.monitor.monitoring.mapper;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.enums.InstanceType;
import com.techvalley.monitor.monitoring.dto.client.InstanceDto;
import com.techvalley.monitor.monitoring.dto.response.MonitoringInstanceResponse;
import org.springframework.stereotype.Component;

@Component
public class MonitoringMapper {

    public MonitoringInstanceResponse toMonitoringInstanceResponse(InstanceDto instance, String warningMessage) {
        if (instance == null) {
            return null;
        }
        return MonitoringInstanceResponse.builder()
                .id(instance.getId())
                .instanceName(instance.getInstanceName())
                .region(instance.getRegion())
                .instanceType(InstanceType.valueOf(instance.getInstanceType()))
                .status(InstanceStatus.valueOf(instance.getStatus()))
                .cpuUsage(instance.getCpuUsage())
                .monthlyCost(instance.getMonthlyCost())
                .clientId(instance.getClientId())
                .launcheAt(instance.getLauncheAt())
                .updateAt(instance.getUpdateAt())
                .warningMessage(warningMessage)
                .build();
    }
}