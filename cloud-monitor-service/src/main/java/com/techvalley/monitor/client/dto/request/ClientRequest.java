package com.techvalley.monitor.client.dto.request;

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
public class ClientRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String name;

    @NotBlank(message = "Gói hợp đồng không được để trống")
    private String contractPlan; // BASIC, STANDARD, PREMIUM

    @NotNull(message = "ID người quản lý không được để trống")
    private Long managerId;

    // Optional fields to match the specification's request example
    private String email;
    private String company;
}
