package com.example.unbox_trade.common.client.user;

import com.example.unbox_trade.common.client.user.dto.UserInfoForSellingBidResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "unbox-user", url = "${user-service.url}")
public interface UserClient {

    @GetMapping("/internal/users/{userId}/for-selling-bid")
    UserInfoForSellingBidResponse getUserInfoForSellingBid(@PathVariable("userId") Long userId);
}
