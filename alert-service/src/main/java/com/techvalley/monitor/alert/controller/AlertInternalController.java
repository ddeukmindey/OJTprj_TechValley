package com.techvalley.monitor.alert.controller;

import com.techvalley.monitor.alert.dto.internal.AlertInternalDto;
import com.techvalley.monitor.alert.dto.internal.CreateAlertInternalRequest;
import com.techvalley.monitor.alert.service.AlertService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal controller – chỉ nhận request từ các service nội bộ (qua X-Internal-Key).
 * Toàn bộ business logic được ủy thác cho AlertService.
 */
@RestController
@RequestMapping("/internal/alerts")
@RequiredArgsConstructor
@Hidden
public class AlertInternalController {

    private final AlertService alertService;

    /** monitoring-service gọi để tạo Alert mới, tự chống trùng (dedup). */
    @PostMapping
    public ResponseEntity<Void> createIfAbsent(@RequestBody CreateAlertInternalRequest request) {
        alertService.createAlertIfAbsent(request);
        return ResponseEntity.ok().build();
    }

    /** monitoring-service gọi để đếm alert chưa resolve, phục vụ /api/monitorings/report. */
    @GetMapping("/count-unresolved")
    public long countUnresolved(@RequestParam(required = false) List<Long> instanceIds) {
        return alertService.countUnresolved(instanceIds);
    }

    /** client-service gọi để tính SLA (loại trừ alert loại CPU_HIGH). */
    @GetMapping("/downtime")
    public List<AlertInternalDto> getDowntimeAlerts(@RequestParam List<Long> instanceIds) {
        return alertService.getDowntimeAlerts(instanceIds);
    }

    /** llm-service gọi để lấy lịch sử alert của 1 instance phục vụ chẩn đoán AI. */
    @GetMapping("/by-instance/{instanceId}")
    public List<AlertInternalDto> getByInstance(@PathVariable Long instanceId) {
        return alertService.getAlertsByInstance(instanceId);
    }
}