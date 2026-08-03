package com.example.matjib.service;

import com.example.matjib.dto.SignupRequest;
import com.example.matjib.exception.BusinessException;
import com.example.matjib.mapper.MemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock MemberMapper memberMapper;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks MemberService memberService;

    @Test
    @DisplayName("회원가입 성공 시 비밀번호가 암호화되어 저장된다")
    void signup_success() {
        // given
        SignupRequest request = new SignupRequest();
        request.setUsername("gildong");
        request.setPassword("1234");
        request.setNickname("홍길동");

        given(memberMapper.countByUsername("gildong")).willReturn(0);
        given(passwordEncoder.encode("1234")).willReturn("encoded_pw");

        // when
        memberService.signup(request);

        // then
        then(passwordEncoder).should().encode("1234");
        then(memberMapper).should().insert(argThat(m ->
                m.getPassword().equals("encoded_pw")       // 평문이 아닌 암호화된 값
                && m.getRole().equals("ROLE_USER")
        ));
    }

    @Test
    @DisplayName("아이디가 중복이면 회원가입에 실패한다")
    void signup_duplicated_username() {
        // given
        SignupRequest request = new SignupRequest();
        request.setUsername("gildong");
        request.setPassword("1234");
        request.setNickname("홍길동");
        given(memberMapper.countByUsername("gildong")).willReturn(1);

        // when & then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 사용 중인 아이디");

        then(memberMapper).should(never()).insert(any());
    }
}
