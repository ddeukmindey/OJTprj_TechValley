package com.techvalley.auth.controller;

import com.techvalley.auth.dto.request.LoginRequest;
import com.techvalley.auth.dto.response.LoginResponse;
import com.techvalley.auth.service.AuthService;
import com.techvalley.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Login successful")
                        .data(response)
                        .build()
        );
    }
}