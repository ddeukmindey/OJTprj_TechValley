package com.techvalley.monitor.client.dto.internal;

import com.techvalley.monitor.client.Client;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientInternalDto {
    private Long id;
    private String name;
    private Long managerId;

    public static ClientInternalDto from(Client client) {
        if (client == null) return null;
        return ClientInternalDto.builder()
                .id(client.getId())
                .name(client.getClientName())
                .managerId(client.getManagerId())
                .build();
    }
}
