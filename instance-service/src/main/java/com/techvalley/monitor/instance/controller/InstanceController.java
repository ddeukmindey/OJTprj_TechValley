package com.techvalley.monitor.instance.controller;

import com.techvalley.common.dto.ApiResponse;
import com.techvalley.common.dto.PageResponse;
import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.dto.request.InstanceRequest;
import com.techvalley.monitor.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.monitor.instance.dto.response.InstanceResponse;
import com.techvalley.monitor.instance.service.InstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InstanceResponse>> createInstance(@Valid @RequestBody InstanceRequest request) {
        InstanceResponse response = instanceService.createInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Thêm mới instance thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InstanceResponse>>> getInstances(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) InstanceStatus status,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        // Map createdAt logic (our entity has launcheAt and updateAt)
        String actualSortBy = "createdAt".equals(sortBy) ? "launcheAt" : sortBy;
        
        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(actualSortBy).ascending() : Sort.by(actualSortBy).descending();
        Pageable pageable = PageRequest.of(page - 1 > 0 ? page - 1 : 0, size, sort);

        PageResponse<InstanceResponse> response = instanceService.getInstances(clientId, status, region, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy danh sách instance thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InstanceResponse>> getInstanceById(@PathVariable Long id) {
        InstanceResponse response = instanceService.getInstanceById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy thông tin chi tiết instance thành công", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InstanceResponse>> updateInstanceStatus(
            @PathVariable Long id,
            @Valid @RequestBody InstanceStatusUpdateRequest request) {
        InstanceResponse response = instanceService.updateInstanceStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật trạng thái thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteInstance(@PathVariable Long id) {
        instanceService.deleteInstance(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Xóa thành công", null));
    }
}
