package com.techvalley.monitor.monitoring.dto.client;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateAlertRequest {
    private Long instanceId;
    private String alertType;
    private String message;
}