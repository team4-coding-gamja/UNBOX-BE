package com.example.unbox_user.user.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSignupResponseDto {

    private Long id;
    private String email;
    private String nickname;
    private String phone;
}
