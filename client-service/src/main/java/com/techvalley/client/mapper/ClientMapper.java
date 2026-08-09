package com.techvalley.client.mapper;

import com.techvalley.client.dto.response.ClientResponse;
import com.techvalley.client.entity.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public ClientResponse toResponse(Client client) {
        if (client == null) {
            return null;
        }
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getClientName())
                .contractPlan(client.getContractPlan())
                .managerId(client.getManagerId())
                .createdAt(client.getCreateAt())
                .build();
    }
}
