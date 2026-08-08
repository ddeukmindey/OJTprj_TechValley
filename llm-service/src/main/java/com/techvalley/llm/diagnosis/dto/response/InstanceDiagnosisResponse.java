package com.techvalley.llm.diagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InstanceDiagnosisResponse {
    private Long instanceId;
    private String instanceName;
    private Integer healthScore;       // 0-100
    private String diagnosis;          // Tóm tắt tình trạng
    private String rootCause;          // Nguyên nhân gốc rễ
    private List<String> actionableSteps; // 3-5 bước khắc phục
    /** "AI" nếu Gemini trả lời thành công, "MOCK" nếu rơi vào fallback rule-based. */
    private String source;
}
