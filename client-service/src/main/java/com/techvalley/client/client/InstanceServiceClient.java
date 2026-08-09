package com.techvalley.client.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.client.dto.client.InstanceDto;
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

    public List<InstanceDto> getInstancesByClientId(Long clientId) {
        try {
            String url = instanceServiceUrl + "/api/instances?clientId=" + clientId + "&size=1000";
            log.info("Gọi REST API sang instance-service cho clientId={}: {}", clientId, url);

            String jsonStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode data = root.has("data") ? root.get("data") : root;
            JsonNode content = data.has("content") ? data.get("content") : data;

            if (content != null && content.isArray()) {
                return objectMapper.convertValue(content, new TypeReference<List<InstanceDto>>() {});
            }
        } catch (Exception e) {
            log.error("Không thể kết nối tới instance-service ({}): {}", instanceServiceUrl, e.getMessage());
        }
        return Collections.emptyList();
    }
}
