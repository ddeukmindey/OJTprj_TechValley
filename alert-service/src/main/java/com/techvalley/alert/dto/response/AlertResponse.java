package com.techvalley.alert.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.techvalley.alert.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AlertResponse {

    private Long id;
    private Long instanceId;
    private AlertType alertType;
    private String message;
    private Boolean isResolved;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;
}
