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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)   // 학습용으로 CSRF 비활성화
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                .requestMatchers("/", "/stores", "/stores/*", "/login", "/signup").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/members/signup").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 리뷰 작성/수정/삭제, 마이페이지, 포인트 등 나머지 전부는 로그인 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")                     // GET: 로그인 화면
                .loginProcessingUrl("/login")            // POST: 로그인 처리 — 같은 경로를 메서드로 구분
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/stores", true)
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
