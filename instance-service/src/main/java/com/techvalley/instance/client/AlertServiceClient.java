package com.techvalley.instance.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertServiceClient {

    private final RestClient restClient;

    @Value("${services.alert-service.url:http://alert-service:8084}")
    private String alertServiceUrl;

    public void deleteAlertsByInstanceId(Long instanceId) {
        try {
            String url = alertServiceUrl + "/api/alerts/instance/" + instanceId;
            log.info("Gọi REST API xóa tất cả Alert thuộc instanceId={} tới alert-service: {}", instanceId, url);

            restClient.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Không thể kết nối tới alert-service ({}): {}", alertServiceUrl, e.getMessage());
        }
    }
}
