package com.example.unbox_order.common.client.user;

import com.example.unbox_order.common.client.user.dto.UserInfoForOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "unbox-user", url = "${user-service.url}", path = "/user")
public interface UserClient {

    @GetMapping("/internal/users/{userId}/for-order")
    UserInfoForOrderResponse getUserInfoForOrder(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/{userId}/has-default-account")
    boolean hasDefaultAccount(@PathVariable("userId") Long userId);
}
