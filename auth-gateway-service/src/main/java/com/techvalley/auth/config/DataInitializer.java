package com.techvalley.auth.config;

import com.techvalley.auth.entity.Member;
import com.techvalley.auth.entity.Role;
import com.techvalley.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Tài khoản ADMIN mặc định
        if (!memberRepository.existsByEmail("admin@techvalley.com")) {
            Member admin = new Member();
            admin.setEmail("admin@techvalley.com");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setName("System Administrator");
            admin.setRole(Role.ADMIN);
            admin.setCreateAt(LocalDateTime.now());
            memberRepository.save(admin);
            System.out.println("✅ Đã tạo tài khoản ADMIN mặc định: admin@techvalley.com");
        }

        // Tài khoản CLIENT_MANAGER mẫu (Lỗi 15)
        if (!memberRepository.existsByEmail("manager@techvalley.com")) {
            Member manager = new Member();
            manager.setEmail("manager@techvalley.com");
            manager.setPassword(passwordEncoder.encode("123456"));
            manager.setName("Client Manager");
            manager.setRole(Role.CLIENT_MANAGER);
            manager.setCreateAt(LocalDateTime.now());
            memberRepository.save(manager);
            System.out.println("✅ Đã tạo tài khoản CLIENT_MANAGER mẫu: manager@techvalley.com");
        }
    }
}