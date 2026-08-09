package com.techvalley.alert.dto.request;

import com.techvalley.alert.enums.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCreateRequest {

    @NotNull(message = "instanceId is required")
    private Long instanceId;

    @NotNull(message = "alertType is required")
    private AlertType alertType;

    @NotBlank(message = "message is required")
    private String message;
}
