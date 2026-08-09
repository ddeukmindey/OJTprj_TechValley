package com.techvalley.monitoring.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.monitoring.dto.client.ClientDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientServiceClient {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  @Value("${services.client-service.url:http://client-service:8082}")
  private String clientServiceUrl;

  public List<ClientDto> getAllClients() {
    try {
      String url = clientServiceUrl + "/api/clients?size=1000";
      log.info("Gọi REST API tới client-service: {}", url);

      String jsonStr = restClient.get()
          .uri(url)
          .retrieve()
          .body(String.class);

      JsonNode root = objectMapper.readTree(jsonStr);
      JsonNode data = root.has("data") ? root.get("data") : root;
      JsonNode items = data.has("items") ? data.get("items") : (data.has("content") ? data.get("content") : data);

      if (items != null && items.isArray()) {
        return objectMapper.convertValue(items, new TypeReference<List<ClientDto>>() {
        });
      }
    } catch (Exception e) {
      log.error("Không thể kết nối tới client-service ({}): {}", clientServiceUrl, e.getMessage());
    }
    return Collections.emptyList();
  }

  public boolean checkClientOwnership(Long clientId, Long managerId) {
    List<Long> managed = getClientIdsByManagerId(managerId);
    return managed.contains(clientId);
  }

  public List<Long> getClientIdsByManagerId(Long managerId) {
    List<ClientDto> clients = getAllClients();
    if (clients == null)
      return Collections.emptyList();
    return clients.stream()
        .filter(c -> c.getManagerId() != null && c.getManagerId().equals(managerId))
        .map(c -> c.getId())
        .toList();
  }
}
