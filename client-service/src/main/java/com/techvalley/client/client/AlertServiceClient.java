package com.techvalley.client.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.client.dto.client.AlertDto;
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
public class AlertServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${services.alert-service.url:http://alert-service:8084}")
    private String alertServiceUrl;

    public List<AlertDto> getAlertsByInstanceId(Long instanceId) {
        try {
            String url = alertServiceUrl + "/api/alerts?instanceId=" + instanceId + "&size=1000";
            log.info("Gọi REST API lấy Alerts của instanceId={} từ alert-service: {}", instanceId, url);

            String jsonStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode data = root.has("data") ? root.get("data") : root;
            JsonNode content = data.has("content") ? data.get("content") : data;

            if (content != null && content.isArray()) {
                return objectMapper.convertValue(content, new TypeReference<List<AlertDto>>() {});
            }
        } catch (Exception e) {
            log.error("Không thể kết nối tới alert-service ({}): {}", alertServiceUrl, e.getMessage());
        }
        return Collections.emptyList();
    }
}
