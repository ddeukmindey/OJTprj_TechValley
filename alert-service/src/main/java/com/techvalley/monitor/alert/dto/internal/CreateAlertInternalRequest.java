package com.techvalley.monitor.alert.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertInternalRequest {
    private Long instanceId;
    private String alertType;
    private String message;
}