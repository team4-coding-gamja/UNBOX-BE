package com.example.unbox_user.user.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminMeUpdateRequestDto {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Pattern(regexp = "^[a-z0-9]{4,10}$", message = "닉네임은 4~10자 영문 소문자, 숫자로 입력해야 합니다.")
    private String nickname;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

}
