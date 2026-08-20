package com.techvalley.monitor.instance.service;

import com.techvalley.common.dto.PageResponse;
import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.dto.internal.InstanceInternalDto;
import com.techvalley.monitor.instance.dto.request.InstanceRequest;
import com.techvalley.monitor.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.monitor.instance.dto.response.InstanceResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InstanceService {

    InstanceResponse createInstance(InstanceRequest request);

    PageResponse<InstanceResponse> getInstances(Long clientId, InstanceStatus status, String region, Pageable pageable);

    InstanceResponse getInstanceById(Long id);

    InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request);

    void deleteInstance(Long id);

    // ── Internal methods (gọi từ InstanceInternalController) ─────────────────

    /** Lấy các instance có cpuUsage cao theo danh sách clientId. */
    List<InstanceInternalDto> getHighCpuInstances(List<Long> clientIds);

    /** Lấy các instance có status ERROR theo danh sách clientId. */
    List<InstanceInternalDto> getErrorInstances(List<Long> clientIds);

    /** Lấy các instance có status STOPPED theo danh sách clientId. */
    List<InstanceInternalDto> getStoppedInstances(List<Long> clientIds);

    /** Lấy tất cả instance, tuỳ chọn lọc theo danh sách clientId. */
    List<InstanceInternalDto> getAllInstances(List<Long> clientIds);

    /** Lấy 1 instance theo ID (dùng cho llm-service). */
    InstanceInternalDto getInstanceInternalById(Long id);
}

