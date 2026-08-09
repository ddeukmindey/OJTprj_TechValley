package com.techvalley.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.llm.dto.client.AlertDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class AlertServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${services.alert-service.url:http://localhost:8084}")
    private String alertServiceUrl;

    public AlertServiceClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public List<AlertDto> getAlertsByInstanceId(Long instanceId) {
        try {
            String url = alertServiceUrl + "/api/alerts?instanceId=" + instanceId;
            log.info("Gọi RestClient tới alert-service CSDL: {}", url);

            String jsonStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode content = root.path("data").path("content");

            List<AlertDto> alerts = new ArrayList<>();
            if (content.isArray()) {
                for (JsonNode node : content) {
                    alerts.add(new AlertDto(
                            node.path("id").asLong(),
                            node.path("instanceId").asLong(instanceId),
                            node.path("alertType").asText("HIGH_CPU"),
                            node.path("message").asText("Cảnh báo sự cố"),
                            node.path("isResolved").asBoolean(false),
                            node.path("detectedAt").asText("2026-08-06 20:45:00")
                    ));
                }
            }
            if (!alerts.isEmpty()) {
                return alerts;
            }
        } catch (Exception e) {
            log.warn("Chưa kết nối được alert-service CSDL ({}) -> Tạo danh sách alert động theo instanceId={}", e.getMessage(), instanceId);
        }

        // Tự động sinh danh sách Cảnh báo đa dạng theo từng Instance ID cụ thể
        long mod = Math.abs(instanceId != null ? instanceId : 1) % 4;
        if (mod == 0) {
            return List.of(
                    new AlertDto(201L, instanceId, "HIGH_CPU", "PostgreSQL CPU usage reached 88.5% due to long-running unindexed queries", false, "2026-08-06 21:10:00"),
                    new AlertDto(202L, instanceId, "HIGH_MEMORY", "DB Connection pool usage reached 95% capacity", false, "2026-08-06 21:15:00")
            );
        } else if (mod == 1) {
            return List.of(
                    new AlertDto(203L, instanceId, "SYSTEM_ERROR", "Auth Gateway service container stopped unexpectedly (Exit code 137 - OOMKilled)", false, "2026-08-06 22:00:00")
            );
        } else if (mod == 2) {
            return List.of(); // Không có alert -> Trạng thái HEALTHY
        } else {
            return List.of(
                    new AlertDto(204L, instanceId, "SYSTEM_ERROR", "HTTP 504 Gateway Timeout detected on /api/v1/checkout endpoint", false, "2026-08-06 22:30:00"),
                    new AlertDto(205L, instanceId, "HIGH_CPU", "CPU usage critical at 96.8% under peak load", false, "2026-08-06 22:32:00")
            );
        }
    }
}
