package com.techvalley.monitor.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalAuthInterceptor implements HandlerInterceptor {

  @Value("${internal.api-key}")
  private String expectedKey;

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) throws Exception {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String key = request.getHeader("X-Internal-Key");
    if (expectedKey == null || expectedKey.isBlank() || !expectedKey.equals(key)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter()
          .write("{\"success\":false,\"code\":403,\"message\":\"Thiếu hoặc sai Internal API Key\",\"data\":null}");
      return false;
    }
    return true;
  }
}