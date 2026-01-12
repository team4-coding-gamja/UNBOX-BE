package com.example.unbox_be.global.client.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserForReviewInfoResponse {

    private Long id;
    private String nickname;
    private String imageUrl;
}
