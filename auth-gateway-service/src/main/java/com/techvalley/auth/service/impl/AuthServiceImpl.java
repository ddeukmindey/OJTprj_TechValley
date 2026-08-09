package com.techvalley.auth.service.impl;

import com.techvalley.auth.dto.request.LoginRequest;
import com.techvalley.auth.dto.response.LoginResponse;
import com.techvalley.auth.entity.Member;
import com.techvalley.auth.repository.MemberRepository;
import com.techvalley.auth.security.JwtTokenProvider;
import com.techvalley.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.techvalley.auth.exception.UnauthorizedException;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                member.getPassword())) {

            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtTokenProvider.generateToken(member);

        return LoginResponse.builder()
                .memberId(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .role(member.getRole())
                .accessToken(accessToken)
                .build();
    }
}