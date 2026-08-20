package com.techvalley.monitor.instance.controller;

import com.techvalley.monitor.instance.dto.internal.InstanceInternalDto;
import com.techvalley.monitor.instance.service.InstanceService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal controller – chỉ nhận request từ các service nội bộ (qua X-Internal-Key).
 * Toàn bộ business logic được ủy thác cho InstanceService.
 */
@RestController
@RequestMapping("/internal/instances")
@RequiredArgsConstructor
@Hidden
public class InstanceInternalController {

    private final InstanceService instanceService;

    @GetMapping("/high-cpu")
    public List<InstanceInternalDto> getHighCpu(@RequestParam(required = false) List<Long> clientIds) {
        return instanceService.getHighCpuInstances(clientIds);
    }

    @GetMapping("/errors")
    public List<InstanceInternalDto> getErrors(@RequestParam(required = false) List<Long> clientIds) {
        return instanceService.getErrorInstances(clientIds);
    }

    @GetMapping("/stopped")
    public List<InstanceInternalDto> getStopped(@RequestParam(required = false) List<Long> clientIds) {
        return instanceService.getStoppedInstances(clientIds);
    }

    @GetMapping
    public List<InstanceInternalDto> getAll(@RequestParam(required = false) List<Long> clientIds) {
        return instanceService.getAllInstances(clientIds);
    }

    @GetMapping("/{id}")
    public InstanceInternalDto getById(@PathVariable Long id) {
        return instanceService.getInstanceInternalById(id);
    }
}