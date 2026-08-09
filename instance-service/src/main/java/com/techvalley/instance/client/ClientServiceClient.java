package com.techvalley.instance.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public boolean checkClientOwnership(Long clientId, Long managerId) {
        try {
            List<Long> managedClientIds = getClientIdsByManagerId(managerId);
            return managedClientIds.contains(clientId);
        } catch (Exception e) {
            log.error("Không thể xác thực quyền sở hữu client từ client-service: {}", e.getMessage());
            return false;
        }
    }

    public List<Long> getClientIdsByManagerId(Long managerId) {
        try {
            String url = clientServiceUrl + "/api/clients?page=1&size=1000";
            log.info("Gọi REST API lấy danh sách Client từ client-service cho managerId={}: {}", managerId, url);

            String jsonStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode data = root.has("data") ? root.get("data") : root;
            JsonNode items = data.has("items") ? data.get("items") : data;

            if (items != null && items.isArray()) {
                List<Long> clientIds = objectMapper.convertValue(items, new TypeReference<List<JsonNode>>() {})
                        .stream()
                        .filter(node -> node.has("managerId") && node.get("managerId").asLong() == managerId)
                        .map(node -> node.get("id").asLong())
                        .toList();
                return clientIds;
            }
        } catch (Exception e) {
            log.error("Không thể kết nối tới client-service ({}): {}", clientServiceUrl, e.getMessage());
        }
        return Collections.emptyList();
    }
}
