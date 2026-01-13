package com.example.unbox_be.global.client.user.dto;

import com.example.unbox_be.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserForReviewInfoResponse {

    private Long id;
    private String nickname;

    public static UserForReviewInfoResponse from(User user) {
        return UserForReviewInfoResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .build();
    }
}
