package com.techvalley.monitor.instance.service.impl;

import com.techvalley.monitor.common.dto.PageMeta;
import com.techvalley.monitor.common.dto.PageResponse;
import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.Instance;
import com.techvalley.monitor.instance.dto.request.InstanceRequest;
import com.techvalley.monitor.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.monitor.instance.dto.response.InstanceResponse;
import com.techvalley.monitor.instance.exception.InstanceNotFoundException;
import com.techvalley.monitor.instance.exception.InvalidInstanceOperationException;
import com.techvalley.monitor.instance.mapper.InstanceMapper;
import com.techvalley.monitor.instance.repository.InstanceRepository;
import com.techvalley.monitor.instance.service.InstanceService;
import com.techvalley.monitor.instance.specification.InstanceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstanceServiceImpl implements InstanceService {

    private final InstanceRepository instanceRepository;
    private final InstanceMapper instanceMapper;

    @Override
    @Transactional
    public InstanceResponse createInstance(InstanceRequest request) {
        Instance instance = instanceMapper.toEntity(request);
        instance = instanceRepository.save(instance);
        return instanceMapper.toResponse(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InstanceResponse> getInstances(Long clientId, InstanceStatus status, String region, Pageable pageable) {
        Specification<Instance> spec = Specification.where(InstanceSpecification.hasClientId(clientId))
                .and(InstanceSpecification.hasStatus(status))
                .and(InstanceSpecification.hasRegion(region));

        Page<Instance> instancePage = instanceRepository.findAll(spec, pageable);
        
        List<InstanceResponse> responses = instancePage.getContent().stream()
                .map(instanceMapper::toResponse)
                .collect(Collectors.toList());

        PageMeta pageMeta = PageMeta.builder()
                .currentPage(instancePage.getNumber() + 1) // PageRequest is 0-indexed, output should be 1-indexed for typical user display
                .pageSize(instancePage.getSize())
                .totalElements(instancePage.getTotalElements())
                .totalPages(instancePage.getTotalPages())
                .build();

        return new PageResponse<>(responses, pageMeta);
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceResponse getInstanceById(Long id) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException(id));
        return instanceMapper.toResponse(instance);
    }

    @Override
    @Transactional
    public InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException(id));

        instance.setStatus(request.getStatus());
        instance.setCpuUsage(request.getCpuUsage());
        
        instance = instanceRepository.save(instance);
        return instanceMapper.toResponse(instance);
    }

    @Override
    @Transactional
    public void deleteInstance(Long id) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException(id));

        if (InstanceStatus.RUNNING.equals(instance.getStatus())) {
            throw new InvalidInstanceOperationException("Không thể xóa máy chủ đang ở trạng thái RUNNING. Vui lòng tắt máy chủ (STOPPED) trước khi xóa.");
        }

        instanceRepository.delete(instance);
    }
}
