package com.techvalley.llm.service.impl;

import com.techvalley.llm.client.AlertServiceClient;
import com.techvalley.llm.client.InstanceServiceClient;
import com.techvalley.llm.dto.client.AlertDto;
import com.techvalley.llm.dto.client.InstanceDto;
import com.techvalley.llm.dto.response.InstanceDiagnosisResponse;
import com.techvalley.llm.service.LlmProviderService;
import com.techvalley.llm.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmServiceImpl.class);

    private final InstanceServiceClient instanceServiceClient;
    private final AlertServiceClient alertServiceClient;
    private final LlmProviderService geminiLlmService;
    private final LlmProviderService mockLlmService;

    @Value("${llm.provider:gemini}")
    private String provider;

    @Value("${llm.mock-enabled:true}")
    private boolean mockEnabled;

    public LlmServiceImpl(InstanceServiceClient instanceServiceClient,
                          AlertServiceClient alertServiceClient,
                          @Qualifier("geminiLlmService") LlmProviderService geminiLlmService,
                          @Qualifier("mockLlmService") LlmProviderService mockLlmService) {
        this.instanceServiceClient = instanceServiceClient;
        this.alertServiceClient = alertServiceClient;
        this.geminiLlmService = geminiLlmService;
        this.mockLlmService = mockLlmService;
    }

    @Override
    public InstanceDiagnosisResponse diagnoseInstance(Long instanceId) {
        log.info("Bắt đầu quy trình Chẩn đoán AI cho instanceId={}, Provider={}", instanceId, provider);

        // 1. Thu thập Context Telemetry
        InstanceDto instance = instanceServiceClient.getInstanceById(instanceId);
        List<AlertDto> alerts = alertServiceClient.getAlertsByInstanceId(instanceId);

        // 2. Thử thực thi với Provider chính (Gemini LLM)
        if ("gemini".equalsIgnoreCase(provider)) {
            try {
                return geminiLlmService.diagnoseInstance(instance, alerts);
            } catch (Exception e) {
                log.warn("Gọi Gemini API thất bại ({})!", e.getMessage());
                if (mockEnabled) {
                    log.info("Cơ chế Fallback được kích hoạt -> Chuyển sang MockLlmServiceImpl");
                    return mockLlmService.diagnoseInstance(instance, alerts);
                }
                throw e;
            }
        }

        // 3. Sử dụng Mock nếu provider được cấu hình là mock
        return mockLlmService.diagnoseInstance(instance, alerts);
    }
}
