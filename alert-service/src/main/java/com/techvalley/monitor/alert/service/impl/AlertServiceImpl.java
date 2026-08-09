package com.techvalley.monitor.alert.service.impl;

import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.alert.dto.request.AlertFilterRequest;
import com.techvalley.monitor.alert.dto.response.AlertResponse;
import com.techvalley.monitor.alert.exception.AlertAlreadyResolvedException;
import com.techvalley.monitor.alert.exception.AlertNotFoundException;
import com.techvalley.monitor.alert.mapper.AlertMapper;
import com.techvalley.monitor.alert.repository.AlertRepository;
import com.techvalley.monitor.alert.service.AlertService;
import com.techvalley.monitor.alert.specification.AlertSpecification;
import com.techvalley.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.techvalley.monitor.alert.dto.internal.ClientDto;
import com.techvalley.monitor.alert.dto.internal.InstanceIdDto;
import com.techvalley.monitor.alert.exception.AccessDeniedException;
import com.techvalley.common.security.UserContext;
import com.techvalley.common.security.UserContextInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("detectedAt", "resolvedAt", "alertType");

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    @Qualifier("clientServiceClient")
    private final WebClient clientServiceClient;
    @Qualifier("instanceServiceClient")
    private final WebClient instanceServiceClient;

    private List<Long> getManagedInstanceIdsIfManager() {
        UserContextInfo user = UserContext.get();
        if (user == null) {
            throw new AccessDeniedException("Người dùng chưa được xác thực");
        }
        if (!"CLIENT_MANAGER".equals(user.getRole())) {
            return null; // ADMIN: không giới hạn
        }

        List<ClientDto> clients = clientServiceClient.get()
                .uri("/internal/clients/by-manager/{managerId}", user.getMemberId())
                .retrieve().bodyToFlux(ClientDto.class).collectList().block();
        List<Long> clientIds = clients != null
                ? clients.stream().map(ClientDto::getId).toList()
                : List.of();
        if (clientIds.isEmpty()) return List.of(-1L); // không quản lý client nào -> không thấy alert nào

        List<InstanceIdDto> instances = instanceServiceClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/instances")
                        .queryParam("clientIds", clientIds).build())
                .retrieve().bodyToFlux(InstanceIdDto.class).collectList().block();
        return instances != null
                ? instances.stream().map(InstanceIdDto::getId).toList()
                : List.of(-1L);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> getAlerts(AlertFilterRequest filter) {
        List<Long> managedInstanceIds = getManagedInstanceIdsIfManager();
        Pageable pageable = buildPageable(filter);

        Specification<Alert> spec = AlertSpecification.withFilter(filter);
        if (managedInstanceIds != null) {
            spec = spec.and(AlertSpecification.byInstanceIdIn(managedInstanceIds));
        }

        Page<Alert> alertPage = alertRepository.findAll(spec, pageable);

        List<AlertResponse> items = alertPage.getContent().stream()
                .map(alertMapper::toResponse)
                .toList();

        return PageResponse.of(items, alertPage);
    }

    @Override
    @Transactional
    public AlertResponse resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

        List<Long> managedInstanceIds = getManagedInstanceIdsIfManager();
        if (managedInstanceIds != null && !managedInstanceIds.contains(alert.getInstanceId())) {
            throw new AccessDeniedException("Bạn không có quyền xử lý cảnh báo này");
        }

        if (alertMapper.isResolved(alert)) {
            throw new AlertAlreadyResolvedException(id);
        }

        alert.setIsResolved(1);
        alert.setResolvedAt(LocalDateTime.now());

        Alert saved = alertRepository.save(alert);
        return alertMapper.toResponse(saved);
    }

    private Pageable buildPageable(AlertFilterRequest filter) {
        // Chuẩn hoá page/size: page phía client là 1-based, Spring Data là 0-based.
        int page = Math.max(filter.getPage(), 1) - 1;
        int size = filter.getSize() <= 0 ? 10 : filter.getSize();

        String sortBy = ALLOWED_SORT_FIELDS.contains(filter.getSortBy()) ? filter.getSortBy() : "detectedAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortOrder())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
