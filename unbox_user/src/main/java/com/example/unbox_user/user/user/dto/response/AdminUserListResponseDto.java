package com.example.unbox_user.user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserListResponseDto {

    private Long id;
    private String email;
    private String nickname;
    private String phone;
    private LocalDateTime createdAt;
}
