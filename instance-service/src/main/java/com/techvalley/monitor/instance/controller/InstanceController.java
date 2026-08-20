package com.techvalley.monitor.instance.controller;

import com.techvalley.common.dto.ApiResponse;
import com.techvalley.common.dto.PageResponse;
import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.dto.request.InstanceRequest;
import com.techvalley.monitor.instance.dto.request.InstanceStatusUpdateRequest;
import com.techvalley.monitor.instance.dto.response.InstanceResponse;
import com.techvalley.monitor.instance.service.InstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Instance API", description = "Đăng ký, truy vấn (phân trang/lọc/sắp xếp), cập nhật trạng thái và xoá máy chủ ảo (instance)")
public class InstanceController {

    private final InstanceService instanceService;

    @PostMapping
    @Operation(summary = "Đăng ký instance mới", description = "Tạo mới một instance thuộc về 1 client. Kiểm tra quyền sở hữu: " +
            "CLIENT_MANAGER chỉ được tạo instance cho client mình quản lý, ADMIN được tạo cho mọi client.")
    public ResponseEntity<ApiResponse<InstanceResponse>> createInstance(@Valid @RequestBody InstanceRequest request) {
        InstanceResponse response = instanceService.createInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Thêm mới instance thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách instance", description = "Hỗ trợ phân trang, lọc theo clientId/status/region và sắp xếp. " +
            "ADMIN xem được toàn bộ, CLIENT_MANAGER chỉ thấy instance thuộc các client được gán cho mình.")
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
    @Operation(summary = "Lấy chi tiết 1 instance", description = "Trả về thông tin đầy đủ của 1 instance theo ID.")
    public ResponseEntity<ApiResponse<InstanceResponse>> getInstanceById(@PathVariable Long id) {
        InstanceResponse response = instanceService.getInstanceById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lấy thông tin chi tiết instance thành công", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái instance", description = "Chuyển trạng thái instance (RUNNING / STOPPED / ERROR).")
    public ResponseEntity<ApiResponse<InstanceResponse>> updateInstanceStatus(
            @PathVariable Long id,
            @Valid @RequestBody InstanceStatusUpdateRequest request) {
        InstanceResponse response = instanceService.updateInstanceStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Cập nhật trạng thái thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá instance", description = "Chỉ cho phép xoá instance ở trạng thái STOPPED hoặc ERROR. " +
            "Instance đang RUNNING sẽ bị chặn xoá (ActiveInstanceException, HTTP 409).")
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        instanceService.deleteInstance(id);
        return ResponseEntity.noContent().build();
    }
}
