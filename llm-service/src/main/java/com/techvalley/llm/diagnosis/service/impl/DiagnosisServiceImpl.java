package com.techvalley.llm.diagnosis.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.llm.common.dto.ApiResponse;
import com.techvalley.llm.common.dto.PageResponse;
import com.techvalley.llm.common.security.UserContext;
import com.techvalley.llm.common.security.UserContextInfo;
import com.techvalley.llm.config.LlmProperties;
import com.techvalley.llm.diagnosis.dto.client.AlertDto;
import com.techvalley.llm.diagnosis.dto.client.ClientDto;
import com.techvalley.llm.diagnosis.dto.client.InstanceDto;
import com.techvalley.llm.diagnosis.dto.response.InstanceDiagnosisResponse;
import com.techvalley.llm.diagnosis.exception.InstanceAccessDeniedException;
import com.techvalley.llm.diagnosis.exception.InstanceNotFoundException;
import com.techvalley.llm.diagnosis.exception.LlmException;
import com.techvalley.llm.diagnosis.service.DiagnosisService;
import com.techvalley.llm.provider.LlmProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Toàn bộ business logic của tính năng "AI Auto-Diagnosis" (Option 1 trong llm_plan.md).
 * Quy trình 5 bước, đúng thứ tự:
 *   1) Lấy dữ liệu Instance từ instance-service (forward JWT của người gọi)
 *   2) Kiểm tra RBAC: CLIENT_MANAGER chỉ được chẩn đoán instance thuộc Client mình quản lý
 *   3) Lấy lịch sử Alert gần nhất từ alert-service
 *   4) Build Prompt -> gọi Gemini, tự động fallback sang Mock nếu lỗi bất kỳ (timeout/hết quota/thiếu key)
 *   5) Parse JSON trả về; nếu AI trả sai định dạng thì fallback an toàn, KHÔNG throw 500 cho người dùng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisServiceImpl implements DiagnosisService {

    @Qualifier("instanceServiceClient")
    private final WebClient instanceServiceClient;
    @Qualifier("alertServiceClient")
    private final WebClient alertServiceClient;
    @Qualifier("clientServiceClient")
    private final WebClient clientServiceClient;

    /** Spring tự gom mọi bean LlmProviderService vào Map, key = tên bean ("gemini", "mock"). */
    private final Map<String, LlmProviderService> providers;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int RECENT_ALERTS_LIMIT = 5;

    /**
     * Retry cho lỗi KẾT NỐI MẠNG (Connection refused/timeout ở tầng TCP) khi gọi các service
     * phụ thuộc - KHÔNG retry lỗi nghiệp vụ (404, 403...) vì đó là phản hồi hợp lệ từ 1 service
     * đang chạy tốt. Lý do cần cái này: Docker Compose "depends_on" chỉ đợi container start,
     * KHÔNG đợi app bên trong sẵn sàng nhận request - mà Spring Boot của instance-service/
     * alert-service/client-service có thể mất 20-35s để khởi động (Hibernate/JPA). Nếu
     * llm-service khởi động nhanh hơn và có request tới ngay, sẽ gặp "Connection refused".
     * Retry với backoff tăng dần tối đa ~40s đủ để chờ qua giai đoạn khởi động đó.
     */
    private static final Retry DOWNSTREAM_RETRY = Retry.backoff(8, Duration.ofSeconds(2))
            .maxBackoff(Duration.ofSeconds(6))
            .filter(ex -> ex instanceof WebClientRequestException)
            .doBeforeRetry(signal -> log.warn(
                    "Service phụ thuộc chưa sẵn sàng (có thể đang khởi động), thử lại lần {}: {}",
                    signal.totalRetries() + 1, signal.failure().getMessage()));

    @Override
    public InstanceDiagnosisResponse diagnose(Long instanceId) {
        UserContextInfo user = UserContext.get();
        String bearerToken = "Bearer " + user.getRawToken();

        // Bước 1: lấy thông tin Instance
        InstanceDto instance = fetchInstance(instanceId, bearerToken);

        // Bước 2: RBAC - CLIENT_MANAGER chỉ được xem instance thuộc client mình quản lý
        enforceAccess(user, instance);

        // Bước 3: lấy lịch sử Alert gần nhất
        List<AlertDto> alerts = fetchRecentAlerts(instanceId, bearerToken);
        long unresolvedCount = alerts.stream().filter(a -> !a.isResolved()).count();

        // Bước 4: build prompt + gọi AI (có fallback)
        String prompt = buildPrompt(instance, alerts, unresolvedCount);
        ProviderResult providerResult = callProviderWithFallback(prompt);

        // Bước 5: parse JSON -> response chuẩn
        return parseResponse(providerResult.rawJson(), instance, providerResult.source());
    }

    // ---------------------------------------------------------------------
    // Bước 1: gọi instance-service
    // ---------------------------------------------------------------------
    private InstanceDto fetchInstance(Long id, String bearerToken) {
        try {
            ApiResponse<InstanceDto> response = instanceServiceClient.get()
                    .uri("/api/instances/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<InstanceDto>>() {})
                    .retryWhen(DOWNSTREAM_RETRY)
                    .block();

            if (response == null || response.getData() == null) {
                throw new InstanceNotFoundException(id);
            }
            return response.getData();
        } catch (WebClientResponseException.NotFound e) {
            throw new InstanceNotFoundException(id);
        }
    }

    // ---------------------------------------------------------------------
    // Bước 2: RBAC Data Isolation (giống logic monitoring-service đang áp dụng)
    // ---------------------------------------------------------------------
    private void enforceAccess(UserContextInfo user, InstanceDto instance) {
        if (!"CLIENT_MANAGER".equals(user.getRole())) {
            return; // ADMIN: toàn quyền
        }
        List<ClientDto> managedClients = clientServiceClient.get()
                .uri("/internal/clients/by-manager/{managerId}", user.getMemberId())
                .retrieve()
                .bodyToFlux(ClientDto.class)
                .retryWhen(DOWNSTREAM_RETRY)
                .collectList()
                .block();

        boolean owns = managedClients != null && managedClients.stream()
                .anyMatch(c -> c.getId() != null && c.getId().equals(instance.getClientId()));

        if (!owns) {
            throw new InstanceAccessDeniedException(
                    "Bạn không có quyền chẩn đoán Instance này vì nó không thuộc Client bạn đang quản lý");
        }
    }

    // ---------------------------------------------------------------------
    // Bước 3: gọi alert-service, lấy 5 alert gần nhất của instance này
    // ---------------------------------------------------------------------
    private List<AlertDto> fetchRecentAlerts(Long instanceId, String bearerToken) {
        ApiResponse<PageResponse<AlertDto>> response = alertServiceClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/alerts")
                        .queryParam("instanceId", instanceId)
                        .queryParam("size", RECENT_ALERTS_LIMIT)
                        .queryParam("sortBy", "detectedAt")
                        .queryParam("sortOrder", "desc")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<PageResponse<AlertDto>>>() {})
                .retryWhen(DOWNSTREAM_RETRY)
                .block();

        if (response == null || response.getData() == null || response.getData().getItems() == null) {
            return List.of();
        }
        return response.getData().getItems();
    }

    // ---------------------------------------------------------------------
    // Bước 4a: build prompt (đúng template trong llm_plan.md mục 4.2)
    // ---------------------------------------------------------------------
    private String buildPrompt(InstanceDto instance, List<AlertDto> alerts, long unresolvedCount) {
        String alertHistory = alerts.isEmpty()
                ? "Không có cảnh báo nào gần đây."
                : alerts.stream()
                        .map(a -> "- [" + a.getAlertType() + "] " + a.getMessage()
                                + " (phát hiện lúc " + (a.getDetectedAt() != null ? a.getDetectedAt().format(TS) : "?")
                                + ", " + (a.isResolved() ? "đã xử lý" : "CHƯA xử lý") + ")")
                        .collect(Collectors.joining("\n"));

        return """
                Bạn là một Chuyên gia Hạ tầng (Senior Cloud Reliability Engineer).
                Nhiệm vụ: phân tích dữ liệu telemetry của máy chủ ảo và lịch sử cảnh báo để tìm nguyên nhân gốc rễ và đề xuất giải pháp xử lý.

                DỮ LIỆU ĐẦU VÀO (TELEMETRY CONTEXT):
                - Tên Instance: %s (ID: %d)
                - Loại cấu hình: %s | Vùng: %s
                - Trạng thái hiện tại: %s
                - Tỷ lệ sử dụng CPU: %s%%
                - Chi phí hàng tháng: $%s
                - Số lượng Alert chưa xử lý: %d
                - Lịch sử Cảnh báo gần nhất:
                %s

                YÊU CẦU ĐẦU RA:
                Chỉ trả về đúng 1 khối JSON theo cấu trúc sau, không thêm lời mở đầu, không thêm markdown code fence:
                {
                  "healthScore": <số nguyên 0-100>,
                  "diagnosis": "<tóm tắt tình trạng máy chủ, 1-2 câu>",
                  "rootCause": "<phân tích nguyên nhân kỹ thuật tiềm ẩn>",
                  "actionableSteps": ["<bước 1>", "<bước 2>", "<bước 3>"]
                }
                """.formatted(
                instance.getName(), instance.getId(),
                instance.getType(), instance.getRegion(),
                instance.getStatus(),
                instance.getCpuUsage(),
                instance.getMonthlyCost(),
                unresolvedCount,
                alertHistory
        );
    }

    // ---------------------------------------------------------------------
    // Bước 4b: gọi Provider theo cấu hình llm.provider, tự fallback sang Mock khi lỗi
    // ---------------------------------------------------------------------
    private ProviderResult callProviderWithFallback(String prompt) {
        boolean forceMock = "mock".equalsIgnoreCase(llmProperties.getProvider());

        if (!forceMock) {
            try {
                String rawJson = providers.get("gemini").generate(prompt);
                return new ProviderResult(rawJson, "AI");
            } catch (Exception primaryFailure) {
                log.warn("Gemini provider thất bại, fallback sang Mock. Lý do: {}", primaryFailure.getMessage());
            }
        }

        try {
            String rawJson = providers.get("mock").generate(prompt);
            return new ProviderResult(rawJson, "MOCK");
        } catch (Exception mockFailure) {
            throw new LlmException("Cả Gemini và Mock provider đều thất bại", mockFailure);
        }
    }

    private record ProviderResult(String rawJson, String source) {}

    // ---------------------------------------------------------------------
    // Bước 5: parse JSON kết quả -> DTO trả về Controller
    // ---------------------------------------------------------------------
    private InstanceDiagnosisResponse parseResponse(String rawJson, InstanceDto instance, String source) {
        try {
            JsonNode node = objectMapper.readTree(stripMarkdownFence(rawJson));

            List<String> steps = new ArrayList<>();
            node.path("actionableSteps").forEach(n -> steps.add(n.asText()));

            return InstanceDiagnosisResponse.builder()
                    .instanceId(instance.getId())
                    .instanceName(instance.getName())
                    .healthScore(node.path("healthScore").asInt(50))
                    .diagnosis(node.path("diagnosis").asText("Không có mô tả"))
                    .rootCause(node.path("rootCause").asText("Không xác định"))
                    .actionableSteps(steps)
                    .source(source)
                    .build();
        } catch (Exception parseError) {
            // Rủi ro #2 trong llm_plan.md: AI trả JSON sai định dạng -> KHÔNG để lỗi 500 lộ ra người dùng,
            // trả về 1 chẩn đoán fallback an toàn kèm nhãn nguồn để Frontend biết mà hiển thị cảnh báo nhẹ.
            log.warn("Không parse được JSON từ provider ({}): {}", source, parseError.getMessage());
            return InstanceDiagnosisResponse.builder()
                    .instanceId(instance.getId())
                    .instanceName(instance.getName())
                    .healthScore(50)
                    .diagnosis("Không thể phân tích chi tiết do lỗi định dạng phản hồi AI.")
                    .rootCause("Không xác định")
                    .actionableSteps(List.of("Vui lòng thử lại sau ít phút."))
                    .source(source + "_FALLBACK")
                    .build();
        }
    }

    private String stripMarkdownFence(String text) {
        if (text == null) return "{}";
        return text.replace("```json", "").replace("```", "").trim();
    }
}
