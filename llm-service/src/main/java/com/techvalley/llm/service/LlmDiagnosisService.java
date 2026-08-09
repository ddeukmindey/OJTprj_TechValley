package com.techvalley.llm.service;

import com.techvalley.llm.dto.external.AlertDto;
import com.techvalley.llm.dto.external.InstanceDto;
import com.techvalley.llm.dto.response.DiagnosisResponse;
import com.techvalley.llm.exception.InstanceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmDiagnosisService {

    @Qualifier("instanceServiceClient")
    private final WebClient instanceServiceClient;
    @Qualifier("alertServiceClient")
    private final WebClient alertServiceClient;

    private final PromptBuilder promptBuilder;
    private final MockDiagnosisEngine mockDiagnosisEngine;
    private final GeminiClientService geminiClientService;

    @Value("${llm.use-real-api:false}")
    private boolean useRealApi;

    public DiagnosisResponse diagnose(Long instanceId) {
        InstanceDto instance = fetchInstance(instanceId);
        List<AlertDto> alertHistory = fetchAlertHistory(instanceId);

        String cause, action, severity, source;

        if (useRealApi) {
            String prompt = promptBuilder.buildDiagnosisPrompt(instance, alertHistory);
            try {
                String[] result = geminiClientService.callGemini(prompt);
                cause = result[0];
                action = result[1];
                severity = result[2];
                source = "GEMINI_AI";
            } catch (Exception e) {
                // Fallback tự động khi gọi API thật thất bại (hết quota, mất mạng, ...)
                String[] mockResult = mockDiagnosisEngine.diagnose(instance, alertHistory);
                cause = mockResult[0]; action = mockResult[1]; severity = mockResult[2];
                source = "MOCK";
            }
        } else {
            String[] mockResult = mockDiagnosisEngine.diagnose(instance, alertHistory);
            cause = mockResult[0]; action = mockResult[1]; severity = mockResult[2];
            source = "MOCK";
        }

        return DiagnosisResponse.builder()
                .instanceId(instance.getId())
                .instanceName(instance.getInstanceName())
                .status(instance.getStatus())
                .cpuUsage(instance.getCpuUsage())
                .cause(cause)
                .recommendedAction(action)
                .severity(severity)
                .source(source)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private InstanceDto fetchInstance(Long instanceId) {
        try {
            return instanceServiceClient.get()
                    .uri("/internal/instances/{id}", instanceId)
                    .retrieve()
                    .bodyToMono(InstanceDto.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            throw new InstanceNotFoundException("Không tìm thấy instance với ID: " + instanceId);
        }
    }

    private List<AlertDto> fetchAlertHistory(Long instanceId) {
        List<AlertDto> result = alertServiceClient.get()
                .uri("/internal/alerts/by-instance/{instanceId}", instanceId)
                .retrieve()
                .bodyToFlux(AlertDto.class)
                .collectList()
                .block();
        return result != null ? result : Collections.emptyList();
    }
}