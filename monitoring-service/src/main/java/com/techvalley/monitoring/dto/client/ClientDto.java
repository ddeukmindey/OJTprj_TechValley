package com.techvalley.monitoring.dto.client;

import com.techvalley.monitoring.enums.ContractPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {
    private Long id;
    private String name;
    private ContractPlan contractPlan;
    private Long managerId;
    private LocalDateTime createdAt;
}
