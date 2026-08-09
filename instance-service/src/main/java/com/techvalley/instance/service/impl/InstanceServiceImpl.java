package com.techvalley.instance.service.impl;

import com.techvalley.instance.enums.InstanceStatus;
import com.techvalley.instance.enums.InstanceType;
import com.techvalley.instance.client.AlertServiceClient;
import com.techvalley.instance.client.ClientServiceClient;

import com.techvalley.instance.dto.request.InstanceRequest;
import com.techvalley.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.instance.dto.response.InstanceResponse;
import com.techvalley.instance.dto.response.PageResponse;
import com.techvalley.instance.entity.Instance;
import com.techvalley.instance.exception.InstanceNotFoundException;
import com.techvalley.instance.exception.InvalidInstanceOperationException;

import com.techvalley.instance.mapper.InstanceMapper;
import com.techvalley.instance.repository.InstanceRepository;
import com.techvalley.instance.security.UserContext;
import com.techvalley.instance.security.UserContextInfo;
import com.techvalley.instance.service.InstanceService;
import com.techvalley.instance.specification.InstanceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InstanceServiceImpl implements InstanceService {

    private final InstanceRepository instanceRepository;
    private final InstanceMapper instanceMapper;
    private final AlertServiceClient alertServiceClient;
    private final ClientServiceClient clientServiceClient;

    private float calculateMonthlyCost(InstanceType type) {
        if (type == null) return 50.0f;
        return switch (type) {
            case SMALL -> 50.0f;
            case MEDIUM -> 120.0f;
            case LARGE -> 250.0f;
        };
    }

    private void validateClientManagerAccess(Long clientId) {
        UserContextInfo user = UserContext.get();
        if (user != null && "CLIENT_MANAGER".equals(user.getRole())) {
            boolean isManagerOfClient = clientServiceClient.checkClientOwnership(clientId, user.getMemberId());
            if (!isManagerOfClient) {
                throw new InvalidInstanceOperationException("Bạn không có quyền quản lý các Máy chủ ảo thuộc về Khách hàng ID: " + clientId);
            }
        }
    }

    @Override
    @Transactional
    public InstanceResponse createInstance(InstanceRequest request) {
        validateClientManagerAccess(request.getClientId());

        Instance instance = new Instance();
        instance.setClientId(request.getClientId());
        instance.setInstanceName(request.getName());
        instance.setInstanceType(request.getType());
        instance.setRegion(request.getRegion() != null ? request.getRegion() : "ap-southeast-1");
        instance.setStatus(request.getStatus() != null ? request.getStatus() : InstanceStatus.RUNNING);
        instance.setCpuUsage(request.getCpuUsage() != null ? request.getCpuUsage() : 0.0f);
        instance.setMonthlyCost(request.getMonthlyCost() != null ? request.getMonthlyCost() : calculateMonthlyCost(request.getType()));
        instance.setLauncheAt(LocalDateTime.now());
        instance.setUpdateAt(LocalDateTime.now());

        Instance saved = instanceRepository.save(instance);
        return instanceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InstanceResponse> getInstances(Long clientId, InstanceStatus status, InstanceType instanceType, String region, String search, String sortBy, String sortDir, int page, int size) {
        UserContextInfo user = UserContext.get();
        if (user != null && "CLIENT_MANAGER".equals(user.getRole())) {
            List<Long> managedClientIds = clientServiceClient.getClientIdsByManagerId(user.getMemberId());
            if (clientId != null) {
                if (!managedClientIds.contains(clientId)) {
                    throw new InvalidInstanceOperationException("Bạn không có quyền xem thông tin Máy chủ ảo của Khách hàng ID: " + clientId);
                }
            } else {
                if (managedClientIds.isEmpty()) {
                    return PageResponse.of(List.of(), Page.empty());
                }
            }
        }

        Specification<Instance> spec = Specification.where(InstanceSpecification.hasClientId(clientId))
                .and(InstanceSpecification.hasStatus(status))
                .and(InstanceSpecification.hasInstanceType(instanceType))
                .and(InstanceSpecification.hasRegion(region))
                .and(InstanceSpecification.searchByName(search));

        // Lỗi 13: Validate sortBy để tránh PropertyReferenceException gây crash 500
        Set<String> allowedSortFields = Set.of("launcheAt", "updateAt", "instanceName", "cpuUsage", "monthlyCost", "status");
        String safeSortBy = (sortBy != null && allowedSortFields.contains(sortBy)) ? sortBy : "launcheAt";
        Sort.Direction direction = (sortDir != null && "asc".equalsIgnoreCase(sortDir)) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, safeSortBy));
        Page<Instance> instancePage = instanceRepository.findAll(spec, pageable);

        List<InstanceResponse> items = instancePage.getContent().stream()
                .map(instanceMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(items, instancePage);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "instances", key = "#id")
    public InstanceResponse getInstanceById(Long id) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException("Không tìm thấy Máy chủ ảo với ID: " + id));
        validateClientManagerAccess(instance.getClientId());
        return instanceMapper.toResponse(instance);
    }

    @Override
    @Transactional
    @CacheEvict(value = "instances", key = "#id")
    public InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException("Không tìm thấy Máy chủ ảo với ID: " + id));

        validateClientManagerAccess(instance.getClientId());

        instance.setStatus(request.getStatus());
        instance.setUpdateAt(LocalDateTime.now());

        if (InstanceStatus.STOPPED.equals(request.getStatus())) {
            instance.setCpuUsage(0.0f);
        }

        Instance saved = instanceRepository.save(instance);
        return instanceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "instances", key = "#id"),
        @CacheEvict(value = "instancesByClient", allEntries = true)
    })
    public void deleteInstance(Long id) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException("Không tìm thấy Máy chủ ảo với ID: " + id));

        validateClientManagerAccess(instance.getClientId());

        if (InstanceStatus.RUNNING.equals(instance.getStatus())) {
            throw new InvalidInstanceOperationException("Không thể xóa Instance đang hoạt động. Vui lòng dừng Instance trước!");
        }

        alertServiceClient.deleteAlertsByInstanceId(id);
        instanceRepository.delete(instance);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "instancesByClient", key = "#clientId")
    public List<InstanceResponse> getInstancesByClientId(Long clientId) {
        return instanceRepository.findByClientId(clientId).stream()
                .map(instanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "instances", key = "#id")
    public InstanceResponse updateCpuUsage(Long id, Float cpuUsage) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException("Không tìm thấy Máy chủ ảo với ID: " + id));

        instance.setCpuUsage(cpuUsage);
        instance.setUpdateAt(LocalDateTime.now());
        Instance saved = instanceRepository.save(instance);
        return instanceMapper.toResponse(saved);
    }
}
