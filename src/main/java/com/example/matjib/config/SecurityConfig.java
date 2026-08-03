package com.example.matjib.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // 비밀번호 암호화 (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)   // 학습용으로 CSRF 비활성화
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                // 화면: 가게 목록/상세, 로그인/회원가입은 누구나
                .requestMatchers("/", "/stores", "/stores/*", "/login", "/signup").permitAll()
                // Swagger + 회원가입 API
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/members/signup").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                // 관리자 전용
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 그 외(리뷰 작성/수정/삭제, 마이페이지, 포인트)는 로그인 필요
                .anyRequest().authenticated()
            )
            // 폼 로그인 - 커스텀 로그인 화면 사용
            .formLogin(form -> form
                .loginPage("/login")                     // GET: 로그인 화면
                .loginProcessingUrl("/login")            // POST: 로그인 처리
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/stores", true)      // 성공 시 가게 목록으로
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/stores")
                .permitAll()
            );

        return http.build();
    }
}
