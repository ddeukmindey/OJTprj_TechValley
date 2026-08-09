package com.techvalley.client.controller;

import com.techvalley.client.dto.client.InstanceDto;
import com.techvalley.client.dto.request.ClientRequest;
import com.techvalley.client.dto.response.*;
import com.techvalley.common.dto.response.ApiResponse;
import com.techvalley.client.exception.AccessDeniedException;
import com.techvalley.client.exception.ClientNotFoundException;
import com.techvalley.client.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Client API", description = "Các API đăng ký, truy vấn danh sách, thống kê chi phí và SLA của khách hàng")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(summary = "Đăng ký khách hàng mới", description = "Chỉ ADMIN mới có quyền tạo mới khách hàng")
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(@Valid @RequestBody ClientRequest request) {
        ClientResponse response = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đăng ký khách hàng thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả khách hàng", description = "ADMIN xem được tất cả, CLIENT_MANAGER chỉ thấy danh sách khách hàng được gán cho mình")
    public ApiResponse<PageResponse<ClientResponse>> getClients(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        return ApiResponse.success("Lấy danh sách khách hàng thành công", clientService.getClients(page, size, search));
    }

    @GetMapping("/{id}/instances")
    @Operation(summary = "Truy vấn danh sách các máy chủ ảo thuộc khách hàng", description = "Lấy danh sách các instance của một khách hàng cụ thể")
    public ApiResponse<List<InstanceDto>> getClientInstances(@PathVariable("id") Long id) {
        return ApiResponse.success("Lấy danh sách máy chủ ảo thành công", clientService.getClientInstances(id));
    }

    @GetMapping("/{id}/cost")
    @Operation(summary = "Tính tổng chi phí hiện tại của khách hàng", description = "Tính tổng chi phí định kỳ tháng hiện tại của khách hàng dựa trên trạng thái các instance")
    public ApiResponse<ClientCostResponse> getClientCost(@PathVariable("id") Long id) {
        return ApiResponse.success("Lấy dữ liệu chi phí thành công", clientService.getClientCost(id));
    }

    @GetMapping("/{id}/cost-forecast")
    @Operation(summary = "Dự báo chi phí cuối tháng hiện tại/tháng tới", description = "Dự báo chi phí cuối tháng dựa trên các máy chủ ảo đang hoạt động (RUNNING)")
    public ApiResponse<ClientCostForecastResponse> getClientCostForecast(@PathVariable("id") Long id) {
        return ApiResponse.success("Tính toán dự báo chi phí thành công", clientService.getClientCostForecast(id));
    }

    @GetMapping("/{id}/sla")
    @Operation(summary = "Tính toán SLA uptime", description = "Tính toán phần trăm SLA hoạt động của khách hàng và so sánh với gói cam kết (PREMIUM/STANDARD/BASIC)")
    public ApiResponse<ClientSlaResponse> getClientSla(@PathVariable("id") Long id) {
        return ApiResponse.success("Tính toán chỉ số SLA thành công", clientService.getClientSla(id));
    }

    // Local Exception Handlers

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleClientNotFound(ClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }
}
