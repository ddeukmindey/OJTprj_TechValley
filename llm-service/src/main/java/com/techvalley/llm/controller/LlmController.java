package com.techvalley.llm.controller;

import com.techvalley.common.dto.ApiResponse;
import com.techvalley.llm.dto.response.DiagnosisResponse;
import com.techvalley.llm.service.LlmDiagnosisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
@Tag(name = "LLM API", description = "Chẩn đoán tự động cho instance bằng AI")
public class LlmController {

    private final LlmDiagnosisService llmDiagnosisService;

    @GetMapping("/{id}/diagnosis")
    @Operation(summary = "Chẩn đoán nguyên nhân & đề xuất hành động cho instance",
            description = "Phân tích trạng thái instance + lịch sử alert, trả về nguyên nhân khả dĩ và hành động đề xuất")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> diagnose(@PathVariable Long id) {
        DiagnosisResponse response = llmDiagnosisService.diagnose(id);
        return ResponseEntity.ok(ApiResponse.success("Chẩn đoán thành công", response));
    }
}