package com.example.matjib.init;

import com.example.matjib.domain.Member;
import com.example.matjib.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 관리자 계정이 없으면 자동 생성.
 * 아이디: admin / 비밀번호: admin1234 (ROLE_ADMIN)
 */
@Slf4j
@Component
@Order(1)   // 다른 시더보다 먼저
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (memberMapper.countByUsername("admin") == 0) {
            Member admin = Member.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin1234"))
                    .nickname("관리자")
                    .role("ROLE_ADMIN")
                    .build();
            memberMapper.insert(admin);
            log.info("[AdminInit] 관리자 계정 생성: admin / admin1234");
        }
    }
}
