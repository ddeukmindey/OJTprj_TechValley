package com.techvalley.monitor.alert.specification;

import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.alert.dto.request.AlertFilterRequest;
import com.techvalley.monitor.enums.AlertType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Chịu trách nhiệm build Specification<Alert> từ AlertFilterRequest.
 * Mỗi điều kiện chỉ được add vào query khi field tương ứng có giá trị (khác null).
 */
public final class AlertSpecification {

    private AlertSpecification() {
    }

    public static Specification<Alert> byAlertType(AlertType alertType) {
        return (root, query, cb) -> alertType == null
                ? null
                : cb.equal(root.get("alertType"), alertType);
    }

    public static Specification<Alert> byInstanceId(Long instanceId) {
        return (root, query, cb) -> instanceId == null
                ? null
                : cb.equal(root.get("instanceId"), instanceId);
    }

    public static Specification<Alert> byResolved(Boolean isResolved) {
        return (root, query, cb) -> isResolved == null
                ? null
                : cb.equal(root.get("isResolved"), isResolved ? 1 : 0);
    }

    /** Lọc theo đúng 1 ngày cụ thể (00:00:00 -> 23:59:59). */
    public static Specification<Alert> byDate(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return null;
            }
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);
            return cb.between(root.get("detectedAt"), start, end);
        };
    }

    /** Lọc theo tháng, định dạng "yyyy-MM". Giá trị sai định dạng sẽ bị bỏ qua (không lọc). */
    public static Specification<Alert> byMonth(String month) {
        return (root, query, cb) -> {
            if (month == null || month.isBlank()) {
                return null;
            }
            try {
                YearMonth ym = YearMonth.parse(month);
                LocalDateTime start = ym.atDay(1).atStartOfDay();
                LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);
                return cb.between(root.get("detectedAt"), start, end);
            } catch (DateTimeParseException ex) {
                return null;
            }
        };
    }

    /** Lọc trong khoảng từ ngày A đến ngày B (bao gồm 2 đầu mút). */
    public static Specification<Alert> byDateRange(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            if (fromDate == null && toDate == null) {
                return null;
            }
            if (fromDate != null && toDate != null) {
                return cb.between(root.get("detectedAt"), fromDate.atStartOfDay(), toDate.atTime(23, 59, 59));
            }
            if (fromDate != null) {
                return cb.greaterThanOrEqualTo(root.get("detectedAt"), fromDate.atStartOfDay());
            }
            return cb.lessThanOrEqualTo(root.get("detectedAt"), toDate.atTime(23, 59, 59));
        };
    }

    public static Specification<Alert> withFilter(AlertFilterRequest filter) {
        return Specification.where(byAlertType(filter.getAlertType()))
                .and(byInstanceId(filter.getInstanceId()))
                .and(byResolved(filter.getIsResolved()))
                .and(byDate(filter.getDate()))
                .and(byMonth(filter.getMonth()))
                .and(byDateRange(filter.getFromDate(), filter.getToDate()));
    }
}
