package com.example.unbox_be.domain.auth.dto.request;

import com.example.unbox_be.domain.admin.entity.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSignupRequestDto {

    @NotBlank(message = "아이디(이메일)는 필수입니다.")
    @Email
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]{8,15}$", message = "비밀번호는 8~15자 영문 대소문자, 숫자, 특수문자로 입력해야 합니다.")
    private String password;

    @NotBlank(message = "사용자 이름은 필수입니다.")
    @Pattern(regexp = "^[a-z0-9]{4,10}$", message = "아이디는 4~10자 영문 소문자, 숫자로 입력해야 합니다.")
    private String nickname;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    @NotNull(message = "관리자 역할은 필수입니다.")
    private AdminRole adminRole; // MANAGER / INSPECTOR
}
