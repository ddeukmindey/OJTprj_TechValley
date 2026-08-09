package com.techvalley.monitor.instance.controller;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.Instance;
import com.techvalley.monitor.instance.dto.internal.InstanceInternalDto;
import com.techvalley.monitor.instance.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/instances")
@RequiredArgsConstructor
public class InstanceInternalController {

    private final InstanceRepository instanceRepository;
    private static final Float CPU_WARNING_THRESHOLD = 80.0f;

    @GetMapping("/high-cpu")
    public List<InstanceInternalDto> getHighCpu(@RequestParam(required = false) List<Long> clientIds) {
        List<Instance> result = (clientIds == null || clientIds.isEmpty())
                ? instanceRepository.findByCpuUsageGreaterThanEqual(CPU_WARNING_THRESHOLD)
                : instanceRepository.findByClientIdInAndCpuUsageGreaterThanEqual(clientIds, CPU_WARNING_THRESHOLD);
        return result.stream().map(InstanceInternalDto::from).toList();
    }

    @GetMapping("/errors")
    public List<InstanceInternalDto> getErrors(@RequestParam(required = false) List<Long> clientIds) {
        List<Instance> result = (clientIds == null || clientIds.isEmpty())
                ? instanceRepository.findByStatus(InstanceStatus.ERROR)
                : instanceRepository.findByClientIdInAndStatus(clientIds, InstanceStatus.ERROR);
        return result.stream().map(InstanceInternalDto::from).toList();
    }

    @GetMapping("/stopped")
    public List<InstanceInternalDto> getStopped(@RequestParam(required = false) List<Long> clientIds) {
        List<Instance> result = (clientIds == null || clientIds.isEmpty())
                ? instanceRepository.findByStatus(InstanceStatus.STOPPED)
                : instanceRepository.findByClientIdInAndStatus(clientIds, InstanceStatus.STOPPED);
        return result.stream().map(InstanceInternalDto::from).toList();
    }

    @GetMapping
    public List<InstanceInternalDto> getAll(@RequestParam(required = false) List<Long> clientIds) {
        List<Instance> result = (clientIds == null || clientIds.isEmpty())
                ? instanceRepository.findAll()
                : instanceRepository.findByClientIdIn(clientIds);
        return result.stream().map(InstanceInternalDto::from).toList();
    }
    @GetMapping("/{id}")
    public InstanceInternalDto getById(@PathVariable Long id) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new com.techvalley.monitor.instance.exception.InstanceNotFoundException(id));
        return InstanceInternalDto.from(instance);
    }
}