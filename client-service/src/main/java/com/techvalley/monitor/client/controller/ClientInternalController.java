package com.techvalley.monitor.client.controller;

import com.techvalley.monitor.client.Client;
import com.techvalley.monitor.client.repository.ClientRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/clients")
@RequiredArgsConstructor
public class ClientInternalController {

    private final ClientRepository clientRepository;

    @GetMapping("/by-manager/{managerId}")
    public List<ClientDto> getByManager(@PathVariable Long managerId) {
        return clientRepository.findByManagerId(managerId).stream()
                .map(ClientDto::from)
                .toList();
    }

    @GetMapping("/count")
    public long count() {
        return clientRepository.count();
    }

    @Data
    public static class ClientDto {
        private Long id;
        private Long managerId;

        public static ClientDto from(Client c) {
            ClientDto dto = new ClientDto();
            dto.setId(c.getId());
            dto.setManagerId(c.getManagerId());
            return dto;
        }
    }
}