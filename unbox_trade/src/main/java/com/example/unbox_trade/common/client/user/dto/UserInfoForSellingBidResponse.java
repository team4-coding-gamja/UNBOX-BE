package com.example.unbox_trade.common.client.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfoForSellingBidResponse {
    private Long userId;
    private String email;
    private String nickname;
    private String phone;

    @Builder
    public UserInfoForSellingBidResponse(Long userId, String email, String nickname, String phone) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.phone = phone;
    }
}
