package com.techvalley.monitoring.security;

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
    private String name;
    private String role;
}
