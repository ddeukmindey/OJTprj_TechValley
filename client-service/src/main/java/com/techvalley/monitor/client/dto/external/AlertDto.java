package com.techvalley.monitor.client.dto.external;

import com.techvalley.monitor.enums.AlertType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertDto {
    private Long id;
    private Long instanceId;
    private AlertType alertType;
    private String message;
    private Integer isResolved;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
}