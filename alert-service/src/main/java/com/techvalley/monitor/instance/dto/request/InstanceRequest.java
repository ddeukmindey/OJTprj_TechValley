package com.techvalley.monitor.instance.dto.request;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.enums.InstanceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InstanceRequest {

    @NotNull(message = "clientId is required")
    private Long clientId;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "region is required")
    private String region;

    @NotNull(message = "type is required")
    private InstanceType type;

    @NotNull(message = "status is required")
    private InstanceStatus status;

    @NotNull(message = "cpuUsage is required")
    @Min(value = 0, message = "cpuUsage must be >= 0")
    private Float cpuUsage;

    @NotNull(message = "monthlyCost is required")
    @Min(value = 0, message = "monthlyCost must be >= 0")
    private Float monthlyCost;
}
