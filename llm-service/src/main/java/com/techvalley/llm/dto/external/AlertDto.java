package com.techvalley.llm.dto.external;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlertDto {
    private Long id;
    private Long instanceId;
    private String alertType;
    private String message;
    private Integer isResolved;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
}