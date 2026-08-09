package com.techvalley.instance.controller;

import com.techvalley.instance.enums.InstanceStatus;
import com.techvalley.instance.enums.InstanceType;
import com.techvalley.instance.dto.request.InstanceRequest;
import com.techvalley.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.common.dto.response.ApiResponse;
import com.techvalley.instance.dto.response.InstanceResponse;
import com.techvalley.instance.dto.response.PageResponse;
import com.techvalley.instance.service.InstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
@Tag(name = "Instance API", description = "Các API quản lý máy chủ ảo (Khởi tạo, Truy vấn, Cập nhật trạng thái, Xóa)")
public class InstanceController {

    private final InstanceService instanceService;

    @PostMapping
    @Operation(summary = "Khởi tạo máy chủ ảo mới", description = "Tạo mới một Instance thuộc về một Khách hàng với cấu hình SMALL, MEDIUM hoặc LARGE")
    public ResponseEntity<ApiResponse<InstanceResponse>> createInstance(@Valid @RequestBody InstanceRequest request) {
        InstanceResponse response = instanceService.createInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Khởi tạo máy chủ ảo thành công", response));
    }

    @GetMapping
    @Operation(summary = "Truy vấn danh sách máy chủ ảo", description = "Lấy danh sách các máy chủ ảo có lọc theo clientId, status, instanceType, region, tìm kiếm theo tên, sắp xếp và phân trang")
    public ApiResponse<PageResponse<InstanceResponse>> getInstances(
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "status", required = false) InstanceStatus status,
            @RequestParam(value = "instanceType", required = false) InstanceType instanceType,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortBy", defaultValue = "launcheAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        return ApiResponse.success("Lấy danh sách máy chủ ảo thành công",
                instanceService.getInstances(clientId, status, instanceType, region, search, sortBy, sortDir, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Truy vấn thông tin chi tiết một máy chủ ảo", description = "Lấy thông tin chi tiết của Instance theo ID")
    public ApiResponse<InstanceResponse> getInstanceById(@PathVariable("id") Long id) {
        return ApiResponse.success("Lấy thông tin chi tiết máy chủ ảo thành công", instanceService.getInstanceById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái máy chủ ảo", description = "Chuyển trạng thái giữa RUNNING, STOPPED, ERROR")
    public ApiResponse<InstanceResponse> updateInstanceStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody InstanceStatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái máy chủ ảo thành công", instanceService.updateInstanceStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa máy chủ ảo", description = "Chỉ cho phép xóa khi Instance ở trạng thái STOPPED hoặc ERROR. Nếu đang RUNNING sẽ bị chặn!")
    public ApiResponse<Void> deleteInstance(@PathVariable("id") Long id) {
        instanceService.deleteInstance(id);
        return ApiResponse.success("Xóa máy chủ ảo ID: " + id + " thành công", null);
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Lấy tất cả Instance theo Client ID", description = "Dùng cho giao tiếp nội bộ giữa các microservices")
    public ApiResponse<List<InstanceResponse>> getInstancesByClientId(@PathVariable("clientId") Long clientId) {
        return ApiResponse.success("Lấy danh sách Instance theo clientId thành công", instanceService.getInstancesByClientId(clientId));
    }

    @PatchMapping("/{id}/cpu")
    @Operation(summary = "Cập nhật % CPU Usage cho Instance", description = "API cho Monitoring Service cập nhật CPU usage")
    public ApiResponse<InstanceResponse> updateCpuUsage(
            @PathVariable("id") Long id,
            @RequestParam("cpuUsage") Float cpuUsage) {
        return ApiResponse.success("Cập nhật chỉ số CPU thành công", instanceService.updateCpuUsage(id, cpuUsage));
    }
}
