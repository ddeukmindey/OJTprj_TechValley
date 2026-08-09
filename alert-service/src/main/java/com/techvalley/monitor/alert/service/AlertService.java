package com.techvalley.monitor.alert.service;

import com.techvalley.monitor.alert.dto.request.AlertFilterRequest;
import com.techvalley.monitor.alert.dto.response.AlertResponse;
import com.techvalley.common.dto.PageResponse;

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
}
