package com.techvalley.monitor.instance.service;

import com.techvalley.monitor.common.dto.PageResponse;
import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.dto.request.InstanceRequest;
import com.techvalley.monitor.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.monitor.instance.dto.response.InstanceResponse;
import org.springframework.data.domain.Pageable;

public interface InstanceService {
    
    InstanceResponse createInstance(InstanceRequest request);
    
    PageResponse<InstanceResponse> getInstances(Long clientId, InstanceStatus status, String region, Pageable pageable);
    
    InstanceResponse getInstanceById(Long id);
    
    InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request);
    
    void deleteInstance(Long id);
}
