package com.techvalley.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Không tìm thấy hoặc định dạng header Authorization không hợp lệ");
            return false;
        }

        String token = authHeader.substring(7).trim();
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build()
                    .parseClaimsJws(token).getBody();
            Long memberId = claims.get("memberId", Long.class);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            UserContext.set(new UserContextInfo(memberId, email, role));
            return true;
        } catch (ExpiredJwtException e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token JWT đã hết hạn");
            return false;
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token JWT không hợp lệ: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"success\":false,\"code\":%d,\"message\":\"%s\",\"data\":null}", status, message));
    }
}