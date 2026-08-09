package com.techvalley.alert.specification;

import com.techvalley.alert.entity.Alert;
import com.techvalley.alert.enums.AlertType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class AlertSpecification {

    private AlertSpecification() {
    }

    public static Specification<Alert> hasInstanceId(Long instanceId) {
        return (root, query, criteriaBuilder) ->
                instanceId == null ? null : criteriaBuilder.equal(root.get("instanceId"), instanceId);
    }

    /** RBAC: Lọc chỉ các Alert có instanceId nằm trong danh sách được phép */
    public static Specification<Alert> hasInstanceIdIn(List<Long> instanceIds) {
        return (root, query, criteriaBuilder) ->
                (instanceIds == null || instanceIds.isEmpty()) ? criteriaBuilder.disjunction()
                        : root.get("instanceId").in(instanceIds);
    }

    public static Specification<Alert> hasAlertType(AlertType alertType) {
        return (root, query, criteriaBuilder) ->
                alertType == null ? null : criteriaBuilder.equal(root.get("alertType"), alertType);
    }

    public static Specification<Alert> hasIsResolved(Boolean isResolved) {
        return (root, query, criteriaBuilder) ->
                isResolved == null ? null : criteriaBuilder.equal(root.get("isResolved"), isResolved);
    }

    public static Specification<Alert> detectedAtBetween(LocalDate fromDate, LocalDate toDate) {
        return (root, query, criteriaBuilder) -> {
            if (fromDate == null && toDate == null) {
                return null;
            }
            if (fromDate != null && toDate != null) {
                LocalDateTime startDateTime = fromDate.atStartOfDay();
                LocalDateTime endDateTime = toDate.atTime(LocalTime.MAX);
                return criteriaBuilder.between(root.get("detectedAt"), startDateTime, endDateTime);
            }
            if (fromDate != null) {
                LocalDateTime startDateTime = fromDate.atStartOfDay();
                return criteriaBuilder.greaterThanOrEqualTo(root.get("detectedAt"), startDateTime);
            }
            LocalDateTime endDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("detectedAt"), endDateTime);
        };
    }
}
