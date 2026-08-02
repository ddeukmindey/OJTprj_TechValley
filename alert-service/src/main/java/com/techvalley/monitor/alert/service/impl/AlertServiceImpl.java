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
import com.techvalley.monitor.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("detectedAt", "resolvedAt", "alertType");

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> getAlerts(AlertFilterRequest filter) {
        Pageable pageable = buildPageable(filter);

        Page<Alert> alertPage = alertRepository.findAll(AlertSpecification.withFilter(filter), pageable);

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
