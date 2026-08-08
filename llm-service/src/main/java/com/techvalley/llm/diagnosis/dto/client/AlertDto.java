package com.techvalley.llm.diagnosis.dto.client;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Map 1:1 field JSON trả về bởi GET /api/alerts của alert-service
 * (xem alert-service/alert/dto/response/AlertResponse.java).
 */
@Data
@NoArgsConstructor
public class AlertDto {
    private Long id;
    private Long instanceId;
    private String alertType;
    private String message;
    private boolean resolved;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
}
