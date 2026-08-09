package com.techvalley.monitor.instance.service.impl;

import com.techvalley.common.dto.PageMeta;
import com.techvalley.common.dto.PageResponse;
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
import com.techvalley.common.security.UserContext;
import com.techvalley.common.security.UserContextInfo;
import com.techvalley.monitor.instance.dto.internal.ClientDto;
import com.techvalley.monitor.instance.exception.AccessDeniedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstanceServiceImpl implements InstanceService {

    private final InstanceRepository instanceRepository;
    private final InstanceMapper instanceMapper;
    @Qualifier("clientServiceClient")
    private final WebClient clientServiceClient;


    private List<Long> getManagedClientIdsIfManager() {
        UserContextInfo user = UserContext.get();
        if (user == null) {
            throw new AccessDeniedException("Người dùng chưa được xác thực");
        }
        if ("CLIENT_MANAGER".equals(user.getRole())) {
            List<ClientDto> clients = clientServiceClient.get()
                    .uri("/internal/clients/by-manager/{managerId}", user.getMemberId())
                    .retrieve()
                    .bodyToFlux(ClientDto.class)
                    .collectList()
                    .block();
            return clients != null ? clients.stream().map(ClientDto::getId).collect(Collectors.toList()) : List.of();
        }
        return null; // null nghĩa là ADMIN, không giới hạn
    }

    private void checkOwnership(Long instanceClientId) {
        List<Long> managedIds = getManagedClientIdsIfManager();
        if (managedIds != null && !managedIds.contains(instanceClientId)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác với instance của khách hàng này");
        }
    }

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
        List<Long> managedIds = getManagedClientIdsIfManager();
        if (managedIds != null) {
            if (clientId != null && !managedIds.contains(clientId)) {
                throw new AccessDeniedException("Bạn không có quyền xem instance của khách hàng này");
            }
            if (clientId == null && managedIds.isEmpty()) {
                return new PageResponse<>(List.of(), PageMeta.builder()
                        .currentPage(1).pageSize(pageable.getPageSize())
                        .totalElements(0).totalPages(0).build());
            }
        }

        Specification<Instance> spec = Specification.where(InstanceSpecification.hasClientId(clientId))
                .and(InstanceSpecification.hasStatus(status))
                .and(InstanceSpecification.hasRegion(region));
        if (managedIds != null && clientId == null) {
            spec = spec.and(InstanceSpecification.hasClientIdIn(managedIds));
        }

        Page<Instance> instancePage = instanceRepository.findAll(spec, pageable);

        List<InstanceResponse> responses = instancePage.getContent().stream()
                .map(instanceMapper::toResponse)
                .collect(Collectors.toList());

        PageMeta pageMeta = PageMeta.builder()
                .currentPage(instancePage.getNumber() + 1)
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
        checkOwnership(instance.getClientId());
        return instanceMapper.toResponse(instance);
    }

    @Override
    @Transactional
    public InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException(id));

        checkOwnership(instance.getClientId());

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
        checkOwnership(instance.getClientId());
        if (InstanceStatus.RUNNING.equals(instance.getStatus())) {
            throw new InvalidInstanceOperationException("Không thể xóa máy chủ đang ở trạng thái RUNNING. Vui lòng tắt máy chủ (STOPPED) trước khi xóa.");
        }

        instanceRepository.delete(instance);
    }
}
