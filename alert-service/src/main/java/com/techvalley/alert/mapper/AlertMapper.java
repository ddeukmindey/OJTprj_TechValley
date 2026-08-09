package com.techvalley.alert.mapper;

import com.techvalley.alert.dto.response.AlertResponse;
import com.techvalley.alert.entity.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

  public AlertResponse toResponse(Alert alert) {
    if (alert == null) {
      return null;
    }

    return AlertResponse.builder()
        .id(alert.getId())
        .instanceId(alert.getInstanceId())
        .alertType(alert.getAlertType())
        .message(alert.getMessage())
        .isResolved(alert.getIsResolved())
        .detectedAt(alert.getDetectedAt())
        .resolvedAt(alert.getResolvedAt())
        .build();
  }
}
