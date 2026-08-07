package com.techvalley.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.llm.dto.client.AlertDto;
import com.techvalley.llm.dto.client.InstanceDto;
import com.techvalley.llm.dto.response.InstanceDiagnosisResponse;
import com.techvalley.llm.service.LlmProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("geminiLlmService")
public class GeminiLlmServiceImpl implements LlmProviderService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmServiceImpl.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gemini-1.5-flash}")
    private String modelName;

    public GeminiLlmServiceImpl(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public InstanceDiagnosisResponse diagnoseInstance(InstanceDto instance, List<AlertDto> alerts) {
        log.info("Gọi GeminiLlmServiceImpl (Google Gemini API: {}) cho instanceId={}", modelName, instance.getId());

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API Key chưa được cấu hình!");
        }

        // 1. Thu thập thông điệp lỗi thực tế
        String alertDetails = (alerts == null || alerts.isEmpty())
                ? "Không có cảnh báo nào gần đây."
                : alerts.stream()
                .map(a -> "- Loại Alert: " + a.getAlertType() + " | Nội dung: " + a.getMessage() + " | DetectedAt: " + a.getDetectedAt())
                .collect(Collectors.joining("\n"));

        // 2. Dựng Dynamic Prompt
        String prompt = String.format("""
            Bạn là một Chuyên gia Hạ tầng Đột xuất (Senior Cloud Reliability Engineer).
            Nhiệm vụ của bạn là phân tích dữ liệu telemetry của máy chủ ảo và danh sách lỗi bắt được để tìm nguyên nhân gốc rễ và đề xuất giải pháp xử lý.

            DỮ LIỆU ĐẦU VÀO (TELEMETRY CONTEXT):
            - Tên Instance: %s (ID: %d)
            - Cấu hình: %s | Vùng: %s
            - Trạng thái hiện tại: %s
            - Tỷ lệ sử dụng CPU: %.2f%%
            - Danh sách Cảnh báo / Lỗi bắt được:
            %s

            YÊU CẦU ĐẦU RÀ:
            Bắt buộc trả về ĐÚNG định dạng JSON tuân theo cấu trúc sau, KHÔNG thêm bất kỳ lời mở đầu hay ký tự markdown ngoài khối JSON:
            {
              "instanceStatus": "%s",
              "issueType": "<Phân loại sự cố chính: HIGH_CPU, SYSTEM_ERROR, HIGH_MEMORY, CRITICAL, NONE>",
              "healthScore": <số nguyên từ 0 đến 100>,
              "diagnosis": "<tóm tắt tình trạng máy chủ>",
              "rootCause": "<nguyên nhân kỹ thuật chi tiết dựa trên lỗi thực tế>",
              "actionableSteps": [
                "<bước 1 khắc phục>",
                "<bước 2 khắc phục>",
                "<bước 3 khắc phục>"
              ]
            }
            """, instance.getInstanceName(), instance.getId(), instance.getInstanceType(),
                instance.getRegion(), instance.getStatus(), instance.getCpuUsage(), alertDetails, instance.getStatus());

        // 3. Gọi Gemini API qua RestClient
        String apiUrl = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", modelName, apiKey);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        String rawResponse = restClient.post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        // 4. Parse kết quả trả về từ Gemini
        return parseGeminiResponse(rawResponse, instance);
    }

    private InstanceDiagnosisResponse parseGeminiResponse(String rawResponse, InstanceDto instance) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawResponse);
            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonText = textNode.asText().trim();
            // Clean markdown code blocks if present (```json ... ```)
            if (jsonText.startsWith("```")) {
                int firstBreak = jsonText.indexOf('\n');
                int lastBreak = jsonText.lastIndexOf("```");
                if (firstBreak != -1 && lastBreak > firstBreak) {
                    jsonText = jsonText.substring(firstBreak + 1, lastBreak).trim();
                }
            }

            JsonNode parsedAi = objectMapper.readTree(jsonText);

            String instanceStatus = parsedAi.path("instanceStatus").asText(instance.getStatus());
            String issueType = parsedAi.path("issueType").asText(instance.getCpuUsage() >= 80.0 ? "HIGH_CPU" : "NONE");
            int healthScore = parsedAi.path("healthScore").asInt(50);
            String diagnosis = parsedAi.path("diagnosis").asText("Đã chẩn đoán xong tình trạng máy chủ.");
            String rootCause = parsedAi.path("rootCause").asText("Chưa xác định được nguyên nhân cụ thể.");
            
            List<String> steps = objectMapper.convertValue(
                    parsedAi.path("actionableSteps"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            return new InstanceDiagnosisResponse(
                    instance.getId(),
                    instance.getInstanceName(),
                    instanceStatus,
                    issueType,
                    healthScore,
                    diagnosis,
                    rootCause,
                    steps,
                    false
            );

        } catch (Exception e) {
            log.error("Lỗi khi parse JSON phản hồi từ Gemini API: {}", e.getMessage());
            throw new RuntimeException("Parse Gemini response failed: " + e.getMessage(), e);
        }
    }
}
