package com.techvalley.monitor.alert.dto.internal;

import com.techvalley.monitor.alert.Alert;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertInternalDto {
    private Long id;
    private Long instanceId;
    private String alertType;
    private String message;
    private Integer isResolved;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;

    public static AlertInternalDto from(Alert a) {
        return AlertInternalDto.builder()
                .id(a.getId())
                .instanceId(a.getInstanceId())
                .alertType(a.getAlertType() != null ? a.getAlertType().name() : null)
                .message(a.getMessage())
                .isResolved(a.getIsResolved())
                .detectedAt(a.getDetectedAt())
                .resolvedAt(a.getResolvedAt())
                .build();
    }
}