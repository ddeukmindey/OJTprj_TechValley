package com.techvalley.alert.controller;

import com.techvalley.alert.dto.request.AlertCreateRequest;
import com.techvalley.alert.dto.request.AlertFilterRequest;
import com.techvalley.alert.dto.response.ApiResponse;
import com.techvalley.alert.dto.response.AlertResponse;
import com.techvalley.alert.dto.response.PageResponse;
import com.techvalley.alert.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert API", description = "Các API quản lý cảnh báo máy chủ ảo (Truy vấn, Giải quyết, Tạo tự động/hàng loạt)")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Truy vấn danh sách cảnh báo", description = "Lấy danh sách cảnh báo có bộ lọc theo instanceId, alertType, isResolved, khoảng thời gian và phân trang")
    public ApiResponse<PageResponse<AlertResponse>> getAlerts(@ModelAttribute AlertFilterRequest filterRequest) {
        return ApiResponse.success("Lấy danh sách cảnh báo thành công", alertService.getAlerts(filterRequest));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Đánh dấu cảnh báo đã xử lý", description = "Đổi trạng thái cảnh báo sang RESOLVED (isResolved=1) và ghi nhận thời gian resolvedAt")
    public ApiResponse<AlertResponse> resolveAlert(@PathVariable("id") Long id) {
        return ApiResponse.success("Đánh dấu xử lý cảnh báo thành công", alertService.resolveAlert(id));
    }

    @PostMapping
    @Operation(summary = "Tạo cảnh báo mới", description = "Tạo 1 cảnh báo đơn lẻ (Kiểm tra chống trùng lặp alert nếu đã có alert chưa xử lý)")
    public ApiResponse<AlertResponse> createAlert(@Valid @RequestBody AlertCreateRequest request) {
        return ApiResponse.success("Tạo cảnh báo thành công", alertService.createAlert(request));
    }

    @PostMapping("/batch")
    @Operation(summary = "Tạo cảnh báo hàng loạt", description = "Tạo danh sách nhiều cảnh báo cùng lúc")
    public ApiResponse<List<AlertResponse>> createAlertsBatch(@Valid @RequestBody List<AlertCreateRequest> requests) {
        return ApiResponse.success("Tạo danh sách cảnh báo hàng loạt thành công", alertService.createAlertsBatch(requests));
    }

    @DeleteMapping("/instance/{instanceId}")
    @Operation(summary = "Xóa tất cả cảnh báo theo instanceId", description = "Xóa toàn bộ các Alert liên quan đến một Instance đã bị xóa")
    public ApiResponse<Void> deleteAlertsByInstanceId(@PathVariable("instanceId") Long instanceId) {
        alertService.deleteAlertsByInstanceId(instanceId);
        return ApiResponse.success("Xóa tất cả cảnh báo thuộc instanceId=" + instanceId + " thành công", null);
    }
}
