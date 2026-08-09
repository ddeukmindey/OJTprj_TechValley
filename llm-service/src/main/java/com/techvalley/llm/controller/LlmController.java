package com.techvalley.llm.controller;

import com.techvalley.common.dto.response.ApiResponse;
import com.techvalley.llm.dto.response.InstanceDiagnosisResponse;
import com.techvalley.llm.service.LlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@Tag(name = "LLM API", description = "Các API phân tích chẩn đoán lỗi và khuyến nghị cho máy chủ ảo bằng AI")
public class LlmController {

    private final LlmService llmService;

    @GetMapping("/diagnose/{instanceId}")
    @Operation(summary = "Phân tích chẩn đoán lỗi Instance", description = "Thu thập thông tin Instance và lịch sử Alert liên quan, gửi tới AI LLM để phân tích nguyên nhân và đưa ra khuyến nghị xử lý")
    public ApiResponse<InstanceDiagnosisResponse> diagnoseInstance(@PathVariable("instanceId") Long instanceId) {
        return ApiResponse.success("Phân tích chẩn đoán Instance bằng AI thành công", llmService.diagnoseInstance(instanceId));
    }
}
