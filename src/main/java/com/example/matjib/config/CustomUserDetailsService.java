package com.example.matjib.config;

import com.example.matjib.domain.Member;
import com.example.matjib.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberMapper memberMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberMapper.findByUsername(username);
        if (member == null) {
            throw new UsernameNotFoundException("존재하지 않는 사용자입니다: " + username);
        }
        // role: "ROLE_USER" -> Security 는 SimpleGrantedAuthority 로 그대로 사용
        return new User(
                member.getUsername(),
                member.getPassword(),
                List.of(new SimpleGrantedAuthority(member.getRole()))
        );
    }
}
