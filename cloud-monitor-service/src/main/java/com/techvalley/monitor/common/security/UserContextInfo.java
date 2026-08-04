package com.techvalley.monitor.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContextInfo {
    private Long memberId;
    private String email;
    private String role; // ADMIN, CLIENT_MANAGER
}
