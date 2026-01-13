package com.example.unbox_be.global.client.user;

import com.example.unbox_be.global.client.user.dto.UserForReviewInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// @FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/user/{id}/exists")
    boolean existsUser(@PathVariable Long id);

    @GetMapping("/internal/users/{id}/for-review")
    UserForReviewInfoResponse getUserInfo(@PathVariable Long id);
}
