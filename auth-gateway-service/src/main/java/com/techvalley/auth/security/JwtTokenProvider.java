package com.techvalley.auth.security;

import com.techvalley.auth.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import com.techvalley.auth.exception.UnauthorizedException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(Member member) {

        Date now = new Date();

        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(member.getEmail())
                .claim("memberId", member.getId())
                .claim("role", member.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Long getMemberId(String token) {
        return getClaims(token)
                .get("memberId", Long.class);
    }

    public String getRole(String token) {
        return getClaims(token)
                .get("role", String.class);
    }

    public boolean validateToken(String token) {

    try {

        Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);

        return true;

    } catch (ExpiredJwtException e) {

        throw new UnauthorizedException("Token has expired");

    } catch (UnsupportedJwtException e) {

        throw new UnauthorizedException("Unsupported token");

    } catch (MalformedJwtException e) {

        throw new UnauthorizedException("Malformed token");

    } catch (SignatureException e) {

        throw new UnauthorizedException("Invalid token signature");

    } catch (IllegalArgumentException e) {

        throw new UnauthorizedException("Token is empty");

    }
}

}