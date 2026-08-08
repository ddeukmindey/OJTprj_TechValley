package com.techvalley.monitor.instance.service.impl;

import com.techvalley.monitor.common.dto.PageMeta;
import com.techvalley.monitor.common.dto.PageResponse;
import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.Instance;
import com.techvalley.monitor.instance.dto.request.InstanceRequest;
import com.techvalley.monitor.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.monitor.instance.dto.response.InstanceResponse;
import com.techvalley.monitor.instance.exception.InstanceNotFoundException;
import com.techvalley.monitor.instance.exception.InstanceUnauthorizedAccessException;
import com.techvalley.monitor.instance.exception.InvalidInstanceOperationException;
import com.techvalley.monitor.instance.mapper.InstanceMapper;
import com.techvalley.monitor.instance.repository.InstanceRepository;
import com.techvalley.monitor.instance.service.InstanceService;
import com.techvalley.monitor.instance.specification.InstanceSpecification;
import com.techvalley.monitor.client.Client;
import com.techvalley.monitor.client.repository.ClientRepository;
import com.techvalley.monitor.common.security.UserContext;
import com.techvalley.monitor.common.security.UserContextInfo;
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
    private final ClientRepository clientRepository;

    private void checkClientManagementPermission(Long clientId) {
        UserContextInfo user = UserContext.get();
        if (user != null && "CLIENT_MANAGER".equals(user.getRole())) {
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null || !user.getMemberId().equals(client.getManagerId())) {
                throw new InstanceUnauthorizedAccessException("Bạn không có quyền thao tác trên máy chủ của khách hàng này.");
            }
        }
    }

    @Override
    @Transactional
    public InstanceResponse createInstance(InstanceRequest request) {
        checkClientManagementPermission(request.getClientId());
        Instance instance = instanceMapper.toEntity(request);
        instance = instanceRepository.save(instance);
        return instanceMapper.toResponse(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InstanceResponse> getInstances(Long clientId, InstanceStatus status, String region, Pageable pageable) {
        UserContextInfo user = UserContext.get();
        List<Long> allowedClientIds = null;
        
        if (user != null && "CLIENT_MANAGER".equals(user.getRole())) {
            if (clientId != null) {
                checkClientManagementPermission(clientId);
            } else {
                List<Client> managedClients = clientRepository.findByManagerId(user.getMemberId());
                allowedClientIds = managedClients.stream().map(Client::getId).collect(Collectors.toList());
                if (allowedClientIds.isEmpty()) {
                    return new PageResponse<>(java.util.Collections.emptyList(), PageMeta.builder().build());
                }
            }
        }

        Specification<Instance> spec = Specification.where(InstanceSpecification.hasStatus(status))
                .and(InstanceSpecification.hasRegion(region));
                
        if (clientId != null) {
            spec = spec.and(InstanceSpecification.hasClientId(clientId));
        } else if (allowedClientIds != null) {
            spec = spec.and(InstanceSpecification.hasClientIdIn(allowedClientIds));
        }

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
        checkClientManagementPermission(instance.getClientId());
        return instanceMapper.toResponse(instance);
    }

    @Override
    @Transactional
    public InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new InstanceNotFoundException(id));
        checkClientManagementPermission(instance.getClientId());

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
        checkClientManagementPermission(instance.getClientId());

        if (InstanceStatus.RUNNING.equals(instance.getStatus())) {
            throw new InvalidInstanceOperationException("Không thể xóa máy chủ đang ở trạng thái RUNNING. Vui lòng tắt máy chủ (STOPPED) trước khi xóa.");
        }

        instanceRepository.delete(instance);
    }
}
