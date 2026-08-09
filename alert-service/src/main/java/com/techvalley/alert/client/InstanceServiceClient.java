package com.techvalley.alert.client;

import com.techvalley.alert.enums.InstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceServiceClient {

    private final RestClient restClient;

    @Value("${services.instance-service.url:http://instance-service:8081}")
    private String instanceServiceUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanceStatusUpdateRequest {
        private InstanceStatus status;
    }

    public void updateInstanceStatusToError(Long instanceId) {
        updateInstanceStatus(instanceId, InstanceStatus.ERROR);
    }

    public void updateInstanceStatusToRunning(Long instanceId) {
        updateInstanceStatus(instanceId, InstanceStatus.RUNNING);
    }

    @SuppressWarnings("null")
    public void updateInstanceStatus(Long instanceId, InstanceStatus status) {
        if (instanceId == null || status == null) {
            return;
        }
        try {
            String url = instanceServiceUrl + "/api/instances/" + instanceId + "/status";
            log.info("Gọi REST API PATCH tới instance-service để cập nhật status instanceId={}: {}", instanceId, status);

            InstanceStatusUpdateRequest request = InstanceStatusUpdateRequest.builder()
                    .status(status)
                    .build();

            restClient.patch()
                    .uri(url)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Lỗi khi gửi request cập nhật status Instance sang instance-service ({}): {}", instanceServiceUrl, e.getMessage());
        }
    }
}
