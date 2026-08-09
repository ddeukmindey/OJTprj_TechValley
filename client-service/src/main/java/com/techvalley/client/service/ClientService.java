package com.techvalley.client.service;

import com.techvalley.client.dto.client.InstanceDto;
import com.techvalley.client.dto.request.ClientRequest;
import com.techvalley.client.dto.response.*;

import java.util.List;

public interface ClientService {

    ClientResponse createClient(ClientRequest request);

    PageResponse<ClientResponse> getClients(int page, int size, String search);

    List<InstanceDto> getClientInstances(Long id);

    ClientCostResponse getClientCost(Long id);

    ClientCostForecastResponse getClientCostForecast(Long id);

    ClientSlaResponse getClientSla(Long id);
}
