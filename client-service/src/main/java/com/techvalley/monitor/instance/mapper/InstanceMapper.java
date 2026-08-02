package com.techvalley.monitor.instance.mapper;

import com.techvalley.monitor.instance.Instance;
import com.techvalley.monitor.instance.dto.request.InstanceRequest;
import com.techvalley.monitor.instance.dto.response.InstanceResponse;
import org.springframework.stereotype.Component;

@Component
public class InstanceMapper {

    public Instance toEntity(InstanceRequest request) {
        if (request == null) return null;
        
        Instance instance = new Instance();
        instance.setClientId(request.getClientId());
        instance.setInstanceName(request.getName());
        instance.setRegion(request.getRegion());
        instance.setInstanceType(request.getType());
        instance.setStatus(request.getStatus());
        instance.setCpuUsage(request.getCpuUsage());
        instance.setMonthlyCost(request.getMonthlyCost());
        
        return instance;
    }

    public InstanceResponse toResponse(Instance instance) {
        if (instance == null) return null;
        
        return InstanceResponse.builder()
                .id(instance.getId())
                .clientId(instance.getClientId())
                .name(instance.getInstanceName())
                .region(instance.getRegion())
                .type(instance.getInstanceType())
                .status(instance.getStatus())
                .cpuUsage(instance.getCpuUsage())
                .monthlyCost(instance.getMonthlyCost())
                .launcheAt(instance.getLauncheAt())
                .updateAt(instance.getUpdateAt())
                .build();
    }
}
