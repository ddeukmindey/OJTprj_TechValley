package com.techvalley.llm.controller;

import com.techvalley.llm.common.dto.ApiResponse;
import com.techvalley.llm.dto.response.InstanceDiagnosisResponse;
import com.techvalley.llm.service.LlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "LLM Feature API", description = "Các REST API Chẩn đoán AI & Phân tích Hạ tầng Máy chủ")
public class LlmController {

    private static final Logger log = LoggerFactory.getLogger(LlmController.class);

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    @GetMapping("/instances/{id}/diagnosis")
    @Operation(summary = "Tự động Chẩn đoán Sự cố Máy chủ bằng AI", 
               description = "Thu thập thông tin Instance và lịch sử Alert gần nhất để Gemini LLM phân tích nguyên nhân gốc rễ và đề xuất phương hướng khắc phục cụ thể.")
    public ResponseEntity<ApiResponse<InstanceDiagnosisResponse>> diagnoseInstance(
            @Parameter(description = "ID của Instance cần chẩn đoán", example = "15")
            @PathVariable("id") Long id) {
        
        log.info("Nhận HTTP GET Request chẩn đoán AI cho instanceId={}", id);
        InstanceDiagnosisResponse response = llmService.diagnoseInstance(id);
        return ResponseEntity.ok(ApiResponse.success(200, "Phân tích chẩn đoán từ AI hoàn tất", response));
    }
}
