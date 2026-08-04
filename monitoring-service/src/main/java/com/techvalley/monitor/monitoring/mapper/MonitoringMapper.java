package com.techvalley.monitor.monitoring.mapper;

import com.techvalley.monitor.instance.Instance;
import com.techvalley.monitor.monitoring.dto.response.MonitoringInstanceResponse;
import org.springframework.stereotype.Component;

@Component
public class MonitoringMapper {

    public MonitoringInstanceResponse toMonitoringInstanceResponse(Instance instance, String warningMessage) {
        if (instance == null) {
            return null;
        }
        return MonitoringInstanceResponse.builder()
                .id(instance.getId())
                .instanceName(instance.getInstanceName())
                .region(instance.getRegion())
                .instanceType(instance.getInstanceType())
                .status(instance.getStatus())
                .cpuUsage(instance.getCpuUsage())
                .monthlyCost(instance.getMonthlyCost())
                .clientId(instance.getClientId())
                .launcheAt(instance.getLauncheAt())
                .updateAt(instance.getUpdateAt())
                .warningMessage(warningMessage)
                .build();
    }
}
