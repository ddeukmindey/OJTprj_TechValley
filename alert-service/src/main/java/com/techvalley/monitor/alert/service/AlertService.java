package com.techvalley.monitor.alert.service;

import com.techvalley.monitor.alert.dto.internal.AlertInternalDto;
import com.techvalley.monitor.alert.dto.internal.CreateAlertInternalRequest;
import com.techvalley.monitor.alert.dto.request.AlertFilterRequest;
import com.techvalley.monitor.alert.dto.response.AlertResponse;
import com.techvalley.common.dto.PageResponse;

import java.util.List;

public interface AlertService {

    /**
     * Use case 1: Xem lịch sử cảnh báo - xem tất cả hoặc có filter
     * (loại, ngày, tháng, khoảng ngày, đã/chưa xử lý, instance).
     */
    PageResponse<AlertResponse> getAlerts(AlertFilterRequest filter);

    /**
     * Use case 2: Đánh dấu 1 cảnh báo chưa xử lý thành đã xử lý.
     */
    AlertResponse resolveAlert(Long id);

    // ── Internal methods (gọi từ AlertInternalController) ──────────────────

    /** Tạo alert nếu chưa tồn tại (dedup). Gọi bởi monitoring-service. */
    void createAlertIfAbsent(CreateAlertInternalRequest request);

    /** Đếm alert chưa resolve, tuỳ chọn lọc theo danh sách instanceId. */
    long countUnresolved(List<Long> instanceIds);

    /** Lấy các alert loại downtime (không phải CPU_HIGH) để tính SLA. */
    List<AlertInternalDto> getDowntimeAlerts(List<Long> instanceIds);

    /** Lấy toàn bộ alert theo instanceId. */
    List<AlertInternalDto> getAlertsByInstance(Long instanceId);
}
