package com.example.unbox_be.domain.admin.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserUpdateRequestDto {

    private String nickname;
    private String phone;
}
