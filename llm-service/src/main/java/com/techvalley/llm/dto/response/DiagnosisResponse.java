package com.techvalley.llm.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DiagnosisResponse {
    private Long instanceId;
    private String instanceName;
    private String status;
    private Float cpuUsage;
    private String cause;
    private String recommendedAction;
    private String severity;      // LOW / MEDIUM / HIGH / CRITICAL
    private String source;        // "MOCK" hoặc "GEMINI_AI"
    private LocalDateTime generatedAt;
}