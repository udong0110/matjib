package com.example.matjib.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Member {
    private Long memberId;
    private String username;
    private String password;
    private String nickname;
    private String role;        // ROLE_USER / ROLE_ADMIN
    private Integer point;      // 보유 포인트
    private LocalDateTime createdAt;

    @Builder
    public Member(String username, String password, String nickname, String role) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }
}
