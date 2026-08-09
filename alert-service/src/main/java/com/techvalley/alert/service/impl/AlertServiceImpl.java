package com.techvalley.alert.service.impl;

import com.techvalley.alert.client.InstanceServiceClient;
import com.techvalley.alert.dto.request.AlertCreateRequest;
import com.techvalley.alert.dto.request.AlertFilterRequest;
import com.techvalley.alert.dto.response.AlertResponse;
import com.techvalley.alert.dto.response.PageResponse;
import com.techvalley.alert.entity.Alert;
import com.techvalley.alert.exception.AlertAlreadyResolvedException;
import com.techvalley.alert.exception.AlertNotFoundException;
import com.techvalley.alert.mapper.AlertMapper;
import com.techvalley.alert.repository.AlertRepository;
import com.techvalley.alert.security.UserContext;
import com.techvalley.alert.security.UserContextInfo;
import com.techvalley.alert.service.AlertService;
import com.techvalley.alert.specification.AlertSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final InstanceServiceClient instanceServiceClient;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> getAlerts(AlertFilterRequest filterRequest) {
        Specification<Alert> spec = Specification.where(AlertSpecification.hasInstanceId(filterRequest.getInstanceId()))
                .and(AlertSpecification.hasAlertType(filterRequest.getAlertType()))
                .and(AlertSpecification.hasIsResolved(filterRequest.getIsResolved()))
                .and(AlertSpecification.detectedAtBetween(filterRequest.getFromDate(), filterRequest.getToDate()));

        // RBAC Data Isolation (Lỗi 6): CLIENT_MANAGER chỉ thấy Alert thuộc Instance của Client mình phụ trách
        UserContextInfo user = UserContext.get();
        if (user != null && "CLIENT_MANAGER".equals(user.getRole())) {
            List<Long> allowedInstanceIds = instanceServiceClient.getInstanceIdsByManagerId(user.getMemberId());
            if (allowedInstanceIds == null || allowedInstanceIds.isEmpty()) {
                // Manager không có Client nào -> trả về danh sách rỗng
                return PageResponse.empty(filterRequest.getPage(), filterRequest.getSize());
            }
            spec = spec.and(AlertSpecification.hasInstanceIdIn(allowedInstanceIds));
        }

        int page = filterRequest.getPage() > 0 ? filterRequest.getPage() - 1 : 0;
        int size = filterRequest.getSize() > 0 ? filterRequest.getSize() : 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<Alert> alertPage = alertRepository.findAll(spec, pageable);

        List<AlertResponse> items = alertPage.getContent().stream()
                .map(alertMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(items, alertPage);
    }

    @Override
    @Transactional
    public AlertResponse resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

        if (Boolean.TRUE.equals(alert.getIsResolved())) {
            throw new AlertAlreadyResolvedException(id);
        }

        alert.setIsResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        Alert saved = alertRepository.save(alert);

        // Fix Lỗi 16: Tự động gọi instance-service đổi status Instance từ ERROR về RUNNING khi Resolve Alert
        instanceServiceClient.updateInstanceStatusToRunning(alert.getInstanceId());

        return alertMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AlertResponse createAlert(AlertCreateRequest request) {
        Optional<Alert> existing = alertRepository.findFirstByInstanceIdAndAlertTypeAndIsResolved(
                request.getInstanceId(), request.getAlertType(), false);

        if (existing.isPresent()) {
            log.info("Deduplication Alert: Đã tồn tại Alert chưa giải quyết cho instanceId={} loại={}",
                    request.getInstanceId(), request.getAlertType());
            return alertMapper.toResponse(existing.get());
        }

        Alert alert = new Alert();
        alert.setInstanceId(request.getInstanceId());
        alert.setAlertType(request.getAlertType());
        alert.setMessage(request.getMessage());
        alert.setIsResolved(false);
        alert.setDetectedAt(LocalDateTime.now());

        Alert saved = alertRepository.save(alert);

        instanceServiceClient.updateInstanceStatusToError(request.getInstanceId());

        return alertMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<AlertResponse> createAlertsBatch(List<AlertCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(this::createAlert)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAlertsByInstanceId(Long instanceId) {
        log.info("Đang xóa tất cả Alert thuộc về instanceId={}", instanceId);
        alertRepository.deleteByInstanceId(instanceId);
    }
}
