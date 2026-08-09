package com.techvalley.instance.dto.request;

import com.techvalley.instance.enums.InstanceStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InstanceStatusUpdateRequest {

    @NotNull(message = "status is required")
    private InstanceStatus status;

    @NotNull(message = "cpuUsage is required")
    @Min(value = 0, message = "cpuUsage must be >= 0")
    @DecimalMax(value = "100.0", message = "cpuUsage must be <= 100")
    private Float cpuUsage;
}
