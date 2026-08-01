package com.techvalley.monitor.alert.dto.request;

import com.techvalley.monitor.enums.AlertType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Bộ lọc cho GET /api/alerts.
 * Áp dụng cho Use case 1 - Xem lịch sử cảnh báo (có chọn lọc):
 *  - Theo loại        -> alertType
 *  - Theo ngày         -> date
 *  - Theo tháng        -> month (yyyy-MM)
 *  - Khoảng ngày A-B   -> fromDate & toDate
 *  - Đã/chưa xử lý     -> isResolved
 *  - Theo instance     -> instanceId
 *
 * Tất cả field đều optional; không truyền field nào -> "xem tất cả".
 */
@Getter
@Setter
public class AlertFilterRequest {

    private AlertType alertType;

    private Long instanceId;

    private Boolean isResolved;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** Định dạng "yyyy-MM", ví dụ 2026-07 */
    private String month;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    private int page = 1;

    private int size = 10;

    /** Cho phép: detectedAt, resolvedAt, alertType */
    private String sortBy = "detectedAt";

    /** asc | desc */
    private String sortOrder = "desc";
}
