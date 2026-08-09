package com.techvalley.monitor.alert.controller;

import com.techvalley.monitor.alert.dto.request.AlertFilterRequest;
import com.techvalley.monitor.alert.dto.response.AlertResponse;
import com.techvalley.monitor.alert.service.AlertService;
import com.techvalley.common.dto.ApiResponse;
import com.techvalley.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller chỉ tiếp nhận HTTP Request / trả response theo API Envelope chuẩn.
 * Toàn bộ business logic nằm ở AlertService (đúng Layered Architecture trong AGENTS.md).
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert", description = "Quản lý lịch sử cảnh báo hệ thống")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Xem lịch sử cảnh báo",
            description = "Xem tất cả hoặc lọc theo loại, ngày, tháng, khoảng ngày, trạng thái xử lý, instance.")
    public ResponseEntity<ApiResponse<PageResponse<AlertResponse>>> getAlerts(
            @Parameter(description = "Bộ lọc: alertType, instanceId, isResolved, date, month, fromDate, toDate, page, size, sortBy, sortOrder")
            @ModelAttribute AlertFilterRequest filter) {

        PageResponse<AlertResponse> result = alertService.getAlerts(filter);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Lấy lịch sử cảnh báo thành công", result)
        );
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Đánh dấu cảnh báo đã xử lý",
            description = "Chuyển 1 cảnh báo đang ở trạng thái chưa xử lý (isResolved = false) sang đã xử lý.")
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(@PathVariable Long id) {

        AlertResponse result = alertService.resolveAlert(id);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Đánh dấu cảnh báo đã xử lý thành công", result)
        );
    }
}
