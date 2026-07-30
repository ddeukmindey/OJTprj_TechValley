package com.techvalley.auth.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String admin() {
        return "Hello Admin";
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('CLIENT_MANAGER')")
    public String manager() {
        return "Hello Manager";
    }

    @GetMapping("/both")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT_MANAGER')")
    public String both() {
        return "Hello Everyone";
    }
}