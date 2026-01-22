package com.example.unbox_user.user.controller;

import com.example.unbox_user.user.dto.internal.UserInfoForOrderResponse;
import com.example.unbox_user.user.dto.internal.UserInfoForSellingBidResponse;
import com.example.unbox_user.user.entity.User;
import com.example.unbox_user.user.repository.UserRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class UserInternalController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}/selling-bid-info")
    public UserInfoForSellingBidResponse getUserInfoForSellingBid(@PathVariable("userId") Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UserInfoForSellingBidResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }

    @GetMapping("/{userId}/order-info")
    public UserInfoForOrderResponse getUserInfoForOrder(@PathVariable("userId") Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 배송지 정보는 현재 User 엔티티에 없거나 미구현 상태인 것으로 보임 (주석 처리됨)
        // 임시로 User 정보 기반으로 채우거나 빈 값으로 둠. 추후 Address 기능 구현 시 연동 필요.
        // 여기서는 예시로 수령인 이름을 닉네임으로, 나머지는 임시 데이터 또는 null 처리
        return UserInfoForOrderResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                // TODO: 실제 배송지 목록에서 기본 배송지를 가져오는 로직 필요
                .receiverName(user.getNickname()) 
                .receiverPhone(user.getPhone())
                .build();
    }
}
