package com.techvalley.llm.diagnosis.controller;

import com.techvalley.llm.common.dto.ApiResponse;
import com.techvalley.llm.diagnosis.dto.response.InstanceDiagnosisResponse;
import com.techvalley.llm.diagnosis.service.DiagnosisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller CHỈ nhận request / trả response theo API Envelope chuẩn.
 * Toàn bộ logic nằm ở DiagnosisServiceImpl (đúng Layered Architecture trong AGENTS.md).
 */
@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
@Tag(name = "LLM Diagnosis", description = "Chẩn đoán tự động Instance bằng AI (Auto Diagnosis)")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @GetMapping("/{id}/diagnosis")
    @Operation(
            summary = "Chẩn đoán AI cho 1 Instance",
            description = "Tổng hợp dữ liệu telemetry (CPU, status) từ instance-service và lịch sử Alert từ " +
                    "alert-service, gửi cho LLM (Gemini, tự fallback sang Mock rule-based nếu lỗi) để trả về " +
                    "healthScore, diagnosis, rootCause và actionableSteps."
    )
    public ResponseEntity<ApiResponse<InstanceDiagnosisResponse>> diagnoseInstance(@PathVariable Long id) {
        InstanceDiagnosisResponse result = diagnosisService.diagnose(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Phân tích chẩn đoán AI hoàn tất", result)
        );
    }
}
