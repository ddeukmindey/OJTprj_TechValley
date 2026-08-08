package com.techvalley.monitor.client.service;

import com.techvalley.monitor.client.dto.request.ClientRequest;
import com.techvalley.monitor.client.dto.response.*;
import com.techvalley.monitor.client.dto.external.InstanceDto;

import java.util.List;

public interface ClientService {

    ClientResponse createClient(ClientRequest request);

    PageResponse<ClientResponse> getClients(int page, int size, String search);

    List<InstanceDto> getClientInstances(Long id);

    ClientCostResponse getClientCost(Long id);

    ClientCostForecastResponse getClientCostForecast(Long id);

    ClientSlaResponse getClientSla(Long id);
}
