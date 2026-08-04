package com.example.matjib.service;

import com.example.matjib.domain.Member;
import com.example.matjib.dto.SignupRequest;
import com.example.matjib.exception.BusinessException;
import com.example.matjib.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(SignupRequest request) {
        if (memberMapper.countByUsername(request.getUsername()) > 0) {
            throw BusinessException.conflict("이미 사용 중인 아이디입니다.");
        }

        Member member = Member.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt 암호화
                .nickname(request.getNickname())
                .role("ROLE_USER")
                .build();

        memberMapper.insert(member);
        log.info("회원가입 완료: username={}, memberId={}", member.getUsername(), member.getMemberId());
        return member.getMemberId();
    }

    public Member getByUsername(String username) {
        Member member = memberMapper.findByUsername(username);
        if (member == null) {
            throw BusinessException.notFound("회원을 찾을 수 없습니다.");
        }
        return member;
    }
}
