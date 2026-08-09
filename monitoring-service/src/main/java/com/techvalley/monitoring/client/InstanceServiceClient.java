package com.techvalley.monitoring.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.monitoring.dto.client.InstanceDto;
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
public class InstanceServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${services.instance-service.url:http://instance-service:8081}")
    private String instanceServiceUrl;

    public List<InstanceDto> getAllInstances() {
        try {
            String url = instanceServiceUrl + "/api/instances?size=1000";
            log.info("Gọi REST API tới instance-service: {}", url);

            String jsonStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode data = root.has("data") ? root.get("data") : root;
            JsonNode items = data.has("items") ? data.get("items") : (data.has("content") ? data.get("content") : data);

            if (items != null && items.isArray()) {
                return objectMapper.convertValue(items, new TypeReference<List<InstanceDto>>() {});
            }
        } catch (Exception e) {
            log.error("Không thể kết nối tới instance-service ({}): {}", instanceServiceUrl, e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<InstanceDto> getInstancesByClientId(Long clientId) {
        try {
            String url = instanceServiceUrl + "/api/instances/client/" + clientId;
            log.info("Gọi REST API lấy danh sách Instance theo clientId={} từ instance-service: {}", clientId, url);

            String jsonStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode data = root.has("data") ? root.get("data") : root;

            if (data != null && data.isArray()) {
                return objectMapper.convertValue(data, new TypeReference<List<InstanceDto>>() {});
            }
        } catch (Exception e) {
            log.error("Không thể kết nối tới instance-service ({}): {}", instanceServiceUrl, e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<InstanceDto> getInstancesByStatus(com.techvalley.monitoring.enums.InstanceStatus status) {
        try {
            String url = instanceServiceUrl + "/api/instances?status=" + status + "&size=1000";
            log.info("Gọi REST API lọc theo status={} tới instance-service: {}", status, url);

            String jsonStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode data = root.has("data") ? root.get("data") : root;
            JsonNode items = data.has("items") ? data.get("items") : (data.has("content") ? data.get("content") : data);

            if (items != null && items.isArray()) {
                return objectMapper.convertValue(items, new TypeReference<List<InstanceDto>>() {});
            }
        } catch (Exception e) {
            log.error("Không thể kết nối tới instance-service ({}): {}", instanceServiceUrl, e.getMessage());
        }
        return Collections.emptyList();
    }
}
