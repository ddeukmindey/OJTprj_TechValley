package com.techvalley.monitor.client.service;

import com.techvalley.monitor.client.dto.internal.ClientInternalDto;
import com.techvalley.monitor.client.dto.request.ClientRequest;
import com.techvalley.monitor.client.dto.response.*;
import com.techvalley.monitor.client.dto.external.InstanceDto;
import org.springframework.lang.NonNull;

import java.util.List;

public interface ClientService {

    ClientResponse createClient(ClientRequest request);

    PageResponse<ClientResponse> getClients(int page, int size, String search);

    List<InstanceDto> getClientInstances(@NonNull Long id);

    ClientCostResponse getClientCost(@NonNull Long id);

    ClientCostForecastResponse getClientCostForecast(@NonNull Long id);

    ClientSlaResponse getClientSla(@NonNull Long id);

    // ── Internal methods ──────────────────────────────────────────────────────

    List<ClientInternalDto> getClientsByManager(Long managerId);

    long countClients();
}
