package com.techvalley.monitoring.client;

import java.util.Objects;

import com.techvalley.monitoring.enums.AlertType;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertServiceClient {

  private final RestClient restClient;

  @Value("${services.alert-service.url:http://alert-service:8084}")
  private String alertServiceUrl;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AlertCreateRequest {
    private Long instanceId;
    private AlertType alertType;
    private String message;
  }

  @SuppressWarnings("null")
  public void createAlert(Long instanceId, AlertType alertType, String message) {
    try {
      String url = alertServiceUrl + "/api/alerts";
      log.info("Gọi REST API POST tới alert-service tạo Alert: instanceId={}, type={}", instanceId, alertType);

      AlertCreateRequest request = AlertCreateRequest.builder()
          .instanceId(instanceId)
          .alertType(alertType)
          .message(message)
          .build();

      restClient.post()
          .uri(url)
          .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
          .body(request)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("Lỗi khi gửi request tạo Alert sang alert-service ({}): {}", alertServiceUrl, e.getMessage());
    }
  }

  @SuppressWarnings("null")
  public void createAlertsBatch(java.util.List<AlertCreateRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return;
    }
    try {
      String url = alertServiceUrl + "/api/alerts/batch";
      log.info("Gọi REST API POST Batch tới alert-service với {} alerts", requests.size());

      restClient.post()
          .uri(url)
          .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
          .body(requests)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("Lỗi khi gửi request tạo Batch Alert sang alert-service ({}): {}", alertServiceUrl, e.getMessage());
    }
  }
}
