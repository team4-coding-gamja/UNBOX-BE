package com.example.unbox_be.domain.user.dto.response;

import com.example.unbox_be.domain.user.entity.User;
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

    public static UserSignupResponseDto from(User user) {
        return UserSignupResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }
}
