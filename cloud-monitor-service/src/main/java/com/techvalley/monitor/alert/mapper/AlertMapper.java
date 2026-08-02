package com.techvalley.monitor.alert.mapper;

import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.alert.dto.response.AlertResponse;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    /**
     * Alert.isResolved đang lưu dạng Integer (0/1) theo entity đã có sẵn,
     * mapper chịu trách nhiệm quy đổi sang boolean cho tầng response.
     */
    public AlertResponse toResponse(Alert alert) {
        if (alert == null) {
            return null;
        }
        return AlertResponse.builder()
                .id(alert.getId())
                .instanceId(alert.getInstanceId())
                .alertType(alert.getAlertType())
                .message(alert.getMessage())
                .resolved(isResolved(alert))
                .detectedAt(alert.getDetectedAt())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }

    public boolean isResolved(Alert alert) {
        return alert.getIsResolved() != null && alert.getIsResolved() == 1;
    }
}
