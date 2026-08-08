package com.techvalley.monitor.alert.controller;

import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.alert.dto.internal.AlertInternalDto;
import com.techvalley.monitor.alert.dto.internal.CreateAlertInternalRequest;
import com.techvalley.monitor.alert.repository.AlertRepository;
import com.techvalley.monitor.enums.AlertType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/internal/alerts")
@RequiredArgsConstructor
public class AlertInternalController {

    private final AlertRepository alertRepository;

    /** monitoring-service gọi để tạo Alert mới, tự chống trùng (dedup) */
    @PostMapping
    public ResponseEntity<Void> createIfAbsent(@RequestBody CreateAlertInternalRequest req) {
        AlertType type = AlertType.valueOf(req.getAlertType());
        Optional<Alert> existing = alertRepository
                .findFirstByInstanceIdAndAlertTypeAndIsResolved(req.getInstanceId(), type, 0);

        if (existing.isEmpty()) {
            Alert alert = new Alert();
            alert.setInstanceId(req.getInstanceId());
            alert.setAlertType(type);
            alert.setMessage(req.getMessage());
            alert.setIsResolved(0);
            alert.setDetectedAt(LocalDateTime.now());
            alertRepository.save(alert);
        }
        return ResponseEntity.ok().build();
    }

    /** monitoring-service gọi để đếm alert chưa resolve, phục vụ /api/monitor/report */
    @GetMapping("/count-unresolved")
    public Long countUnresolved(@RequestParam(required = false) List<Long> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return alertRepository.countByIsResolved(0);
        }
        return alertRepository.countByInstanceIdInAndIsResolved(instanceIds, 0);
    }

    /** client-service gọi để tính SLA (loại trừ alert loại CPU_HIGH) — dùng ở bước sau */
    @GetMapping("/downtime")
    public List<AlertInternalDto> getDowntimeAlerts(@RequestParam List<Long> instanceIds) {
        List<Alert> alerts = alertRepository.findByInstanceIdInAndAlertTypeNot(instanceIds, AlertType.CPU_HIGH);
        return alerts.stream().map(AlertInternalDto::from).toList();
    }
}