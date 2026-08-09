package com.techvalley.client.dto.response;

import com.techvalley.client.enums.ContractPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {
    private Long id;
    private String name;
    private String email;
    private String company;
    private ContractPlan contractPlan;
    private Long managerId;
    private LocalDateTime createdAt;
}
