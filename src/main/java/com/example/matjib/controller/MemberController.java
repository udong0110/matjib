package com.example.matjib.controller;

import com.example.matjib.dto.SignupRequest;
import com.example.matjib.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "회원", description = "회원가입 / 로그인 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입", description = "아이디/비밀번호/닉네임으로 회원가입합니다. 비밀번호는 BCrypt로 암호화 저장됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest request) {
        Long memberId = memberService.signup(request);
        return ResponseEntity.ok(Map.of("memberId", memberId, "message", "회원가입이 완료되었습니다."));
    }
}
