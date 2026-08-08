package com.techvalley.llm.common.security;

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
    /**
     * Token JWT gốc (chưa gồm tiền tố "Bearer ") của request hiện tại.
     * llm-service KHÔNG tự phát hành token mới - nó chỉ đóng vai trò "khách hàng nội bộ"
     * và phải forward lại đúng token này khi gọi sang instance-service / alert-service / client-service,
     * để 2 service kia tự xác thực & tự áp RBAC như khi người dùng gọi trực tiếp.
     */
    private String rawToken;
}
