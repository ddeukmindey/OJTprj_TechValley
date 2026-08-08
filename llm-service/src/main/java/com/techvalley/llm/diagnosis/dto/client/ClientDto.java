package com.techvalley.llm.diagnosis.dto.client;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Map response của GET /internal/clients/by-manager/{managerId} (client-service),
 * endpoint nội bộ đã tồn tại sẵn - monitoring-service cũng đang dùng endpoint này
 * để lọc RBAC cho CLIENT_MANAGER (xem monitoring-service/MonitoringServiceImpl).
 */
@Data
@NoArgsConstructor
public class ClientDto {
    private Long id;
    private Long managerId;
}
