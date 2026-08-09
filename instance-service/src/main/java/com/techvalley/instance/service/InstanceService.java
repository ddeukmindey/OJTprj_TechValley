package com.techvalley.instance.service;

import com.techvalley.instance.enums.InstanceStatus;
import com.techvalley.instance.enums.InstanceType;
import com.techvalley.instance.dto.request.InstanceRequest;
import com.techvalley.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.instance.dto.response.InstanceResponse;
import com.techvalley.instance.dto.response.PageResponse;

import java.util.List;

public interface InstanceService {

    InstanceResponse createInstance(InstanceRequest request);

    PageResponse<InstanceResponse> getInstances(Long clientId, InstanceStatus status, InstanceType instanceType, String region, String search, int page, int size);

    InstanceResponse getInstanceById(Long id);

    InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request);

    void deleteInstance(Long id);

    List<InstanceResponse> getInstancesByClientId(Long clientId);

    InstanceResponse updateCpuUsage(Long id, Float cpuUsage);
}
