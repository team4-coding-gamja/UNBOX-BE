package com.example.unbox_order.common.client.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfoForOrderResponse {
    private Long userId;
    private String email;
    private String nickname;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String receiverZipCode;

    @Builder
    public UserInfoForOrderResponse(Long userId, String email, String nickname, String receiverName, String receiverPhone, String receiverAddress, String receiverZipCode) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.receiverZipCode = receiverZipCode;
    }
}
