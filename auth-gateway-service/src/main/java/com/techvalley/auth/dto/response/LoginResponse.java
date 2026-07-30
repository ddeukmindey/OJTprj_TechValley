package com.techvalley.auth.dto.response;

import com.techvalley.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private Long memberId;

    private String name;

    private String email;

    private Role role;

    private String accessToken;

}