package com.techvalley.monitoring.controller;

import com.techvalley.monitoring.enums.InstanceStatus;
import com.techvalley.monitoring.dto.response.ApiResponse;
import com.techvalley.monitoring.dto.response.MonitoringInstanceResponse;
import com.techvalley.monitoring.dto.response.MonitoringReportResponse;
import com.techvalley.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring API", description = "Các API giám sát hiệu năng máy chủ ảo và xuất báo cáo hệ thống")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/instances")
    @Operation(summary = "Truy vấn danh sách máy chủ ảo kèm cảnh báo CPU", description = "ADMIN xem toàn bộ, CLIENT_MANAGER xem danh sách máy chủ thuộc về khách hàng của mình")
    public ApiResponse<List<MonitoringInstanceResponse>> getMonitoredInstances(
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "status", required = false) InstanceStatus status,
            @RequestParam(value = "cpuThreshold", required = false) Float cpuThreshold) {
        return ApiResponse.success("Truy vấn thông tin giám sát thành công",
                monitoringService.getMonitoredInstances(clientId, status, cpuThreshold));
    }

    @GetMapping("/report")
    @Operation(summary = "Xuất báo cáo tổng quan giám sát hệ thống", description = "Thống kê tổng số instance, số instance đang chạy/lỗi/quá tải CPU và danh sách cần chú ý")
    public ApiResponse<MonitoringReportResponse> getMonitoringReport(
            @RequestParam(value = "clientId", required = false) Long clientId) {
        return ApiResponse.success("Xuất báo cáo giám sát hệ thống thành công",
                monitoringService.getMonitoringReport(clientId));
    }
}
