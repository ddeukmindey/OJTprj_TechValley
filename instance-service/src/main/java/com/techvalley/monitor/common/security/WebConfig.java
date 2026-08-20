package com.techvalley.monitor.common.security;

import com.techvalley.common.security.JwtInterceptor;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final @NonNull JwtInterceptor jwtInterceptor;
  private final @NonNull InternalAuthInterceptor internalAuthInterceptor;

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(jwtInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/login",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html");
    registry.addInterceptor(internalAuthInterceptor)
        .addPathPatterns("/internal/**");
  }
}
