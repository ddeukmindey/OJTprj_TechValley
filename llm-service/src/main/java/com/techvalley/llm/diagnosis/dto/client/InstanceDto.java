package com.techvalley.llm.diagnosis.dto.client;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Map 1:1 các field JSON trả về bởi GET /api/instances/{id} của instance-service
 * (xem instance-service/instance/dto/response/InstanceResponse.java).
 * Field không dùng tới vẫn được khai báo để Jackson không cần "unknown property" config.
 */
@Data
@NoArgsConstructor
public class InstanceDto {
    private Long id;
    private Long clientId;
    private String name;
    private String region;
    private String type;      // SMALL / MEDIUM / LARGE
    private String status;    // RUNNING / STOPPED / ERROR
    private Float cpuUsage;
    private Float monthlyCost;
    private LocalDateTime launcheAt;
    private LocalDateTime updateAt;
}
