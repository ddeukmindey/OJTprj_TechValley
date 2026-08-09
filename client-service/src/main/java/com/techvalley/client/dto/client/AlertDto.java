package com.techvalley.client.dto.client;

import com.techvalley.client.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private Long id;
    private Long instanceId;
    private AlertType alertType;
    private String message;
    private Boolean isResolved;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
}
