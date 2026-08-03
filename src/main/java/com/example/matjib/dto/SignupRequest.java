package com.example.matjib.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원가입 요청")
public class SignupRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
    @Schema(description = "로그인 아이디", example = "gildong")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 20, message = "비밀번호는 4~20자여야 합니다.")
    @Schema(description = "비밀번호", example = "1234")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;
}
