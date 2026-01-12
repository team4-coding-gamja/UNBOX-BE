package com.example.unbox_be.global.client.user;

import com.example.unbox_be.global.client.user.dto.UserForReviewInfoResponse;

public interface UserClient {

    UserForReviewInfoResponse getUserInfo(Long userId);
}
