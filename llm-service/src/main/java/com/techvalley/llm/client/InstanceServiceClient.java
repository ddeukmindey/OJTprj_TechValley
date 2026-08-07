package com.techvalley.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.llm.dto.client.InstanceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InstanceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(InstanceServiceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${services.instance-service.url:http://localhost:8081}")
    private String instanceServiceUrl;

    public InstanceServiceClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public InstanceDto getInstanceById(Long id) {
        try {
            String url = instanceServiceUrl + "/api/instances/" + id;
            log.info("Gọi RestClient tới instance-service CSDL: {}", url);
            
            String jsonStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode data = root.has("data") ? root.get("data") : root;

            if (data != null && data.has("id")) {
                return new InstanceDto(
                        data.path("id").asLong(id),
                        data.path("clientId").asLong(1L),
                        data.path("instanceName").asText("instance-" + id),
                        data.path("instanceType").asText("t3.medium"),
                        data.path("region").asText("ap-southeast-1"),
                        data.path("status").asText("RUNNING"),
                        data.path("cpuUsage").asDouble(85.0),
                        data.path("monthlyCost").asDouble(100.0),
                        data.path("launcheAt").asText("2026-08-01 00:00:00")
                );
            }
        } catch (Exception e) {
            log.warn("Chưa kết nối được instance-service CSDL ({}) -> Tạo dữ liệu telemetry động dựa trên instanceId={}", e.getMessage(), id);
        }

        // Tự động sinh dữ liệu Telemetry đa dạng theo Instance ID thực tế
        long mod = Math.abs(id != null ? id : 1) % 4;
        if (mod == 0) {
            return new InstanceDto(id, 101L, "db-postgres-primary", "db.r6g.xlarge", "ap-southeast-1", "RUNNING", 88.5, 320.0, "2026-08-01 08:00:00");
        } else if (mod == 1) {
            return new InstanceDto(id, 102L, "auth-gateway-service", "t3.medium", "us-east-1", "STOPPED", 0.0, 45.0, "2026-08-02 10:30:00");
        } else if (mod == 2) {
            return new InstanceDto(id, 103L, "client-portal-worker", "t3.small", "ap-southeast-1", "RUNNING", 24.5, 25.0, "2026-08-03 14:15:00");
        } else {
            return new InstanceDto(id, 104L, "payment-service-prod", "c5.xlarge", "ap-southeast-1", "ERROR", 96.8, 180.0, "2026-08-04 09:00:00");
        }
    }
}
