package com.techvalley.monitoring.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String memberIdHeader = request.getHeader("X-User-Id");
        String nameHeader = request.getHeader("X-User-Name");
        String roleHeader = request.getHeader("X-User-Role");

        if (memberIdHeader != null && roleHeader != null) {
            try {
                Long memberId = Long.parseLong(memberIdHeader);
                UserContextInfo userInfo = UserContextInfo.builder()
                        .memberId(memberId)
                        .name(nameHeader)
                        .role(roleHeader)
                        .build();
                UserContext.set(userInfo);
            } catch (NumberFormatException ignored) {
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
