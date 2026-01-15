package com.example.unbox_be.domain.user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserMeUpdateResponseDto {

    private Long id;
    private String email;
    private String nickname;
    private String phone;
}
