package com.techvalley.monitor.monitoring.controller;

import com.techvalley.monitor.common.dto.ApiResponse;
import com.techvalley.monitor.monitoring.dto.response.MonitoringInstanceResponse;
import com.techvalley.monitor.monitoring.dto.response.MonitoringReportResponse;
import com.techvalley.monitor.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Tag(name = "Monitoring API", description = "Các API giám sát hạ tầng máy chủ ảo và cảnh báo tự động")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/warnings")
    @Operation(summary = "Danh sách cảnh báo CPU cao", description = "Quét hệ thống và trả về danh sách instance có cpuUsage >= 80%, tự động tạo Alert HIGH_CPU")
    public ApiResponse<List<MonitoringInstanceResponse>> getWarnings() {
        return ApiResponse.success(monitoringService.getWarnings());
    }

    @GetMapping("/errors")
    @Operation(summary = "Danh sách máy chủ bị lỗi", description = "Quét hệ thống và trả về danh sách instance có status == ERROR, tự động tạo Alert SYSTEM_ERROR")
    public ApiResponse<List<MonitoringInstanceResponse>> getErrors() {
        return ApiResponse.success(monitoringService.getErrors());
    }

    @GetMapping("/long-stopped")
    @Operation(summary = "Danh sách máy chủ tạm dừng lâu ngày", description = "Lấy danh sách các instance bị tạm dừng (STOPPED) ít nhất 48 giờ")
    public ApiResponse<List<MonitoringInstanceResponse>> getLongStopped() {
        return ApiResponse.success(monitoringService.getLongStopped());
    }

    @GetMapping("/report")
    @Operation(summary = "Báo cáo tổng quan hạ tầng", description = "Báo cáo tổng số instance, số lượng từng trạng thái, CPU trung bình và tổng số alert chưa xử lý")
    public ApiResponse<MonitoringReportResponse> getReport() {
        return ApiResponse.success(monitoringService.getOverviewReport());
    }
}
