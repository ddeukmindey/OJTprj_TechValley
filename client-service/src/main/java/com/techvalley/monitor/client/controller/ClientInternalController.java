package com.techvalley.monitor.client.controller;

import com.techvalley.monitor.client.dto.internal.ClientInternalDto;
import com.techvalley.monitor.client.service.ClientService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal controller – chỉ nhận request từ các service nội bộ (qua X-Internal-Key).
 * Toàn bộ business logic được ủy thác cho ClientService.
 */
@RestController
@RequestMapping("/internal/clients")
@RequiredArgsConstructor
@Hidden
public class ClientInternalController {

    private final ClientService clientService;

    /** Lấy danh sách client theo managerId. Gọi bởi instance-service, alert-service để phân quyền. */
    @GetMapping("/by-manager/{managerId}")
    public List<ClientInternalDto> getByManager(@PathVariable Long managerId) {
        return clientService.getClientsByManager(managerId);
    }

    /** Đếm tổng số client. */
    @GetMapping("/count")
    public long count() {
        return clientService.countClients();
    }
}