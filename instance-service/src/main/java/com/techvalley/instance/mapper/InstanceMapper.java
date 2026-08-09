package com.techvalley.instance.mapper;

import com.techvalley.instance.dto.response.InstanceResponse;
import com.techvalley.instance.entity.Instance;
import org.springframework.stereotype.Component;

@Component
public class InstanceMapper {

    public InstanceResponse toResponse(Instance instance) {
        if (instance == null) {
            return null;
        }

        return InstanceResponse.builder()
                .id(instance.getId())
                .clientId(instance.getClientId())
                .name(instance.getInstanceName())
                .type(instance.getInstanceType())
                .region(instance.getRegion())
                .status(instance.getStatus())
                .cpuUsage(instance.getCpuUsage())
                .monthlyCost(instance.getMonthlyCost())
                .launcheAt(instance.getLauncheAt())
                .updateAt(instance.getUpdateAt())
                .build();
    }
}
