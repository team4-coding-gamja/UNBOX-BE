package com.example.unbox_user.user.controller;

import com.example.unbox_user.user.dto.internal.UserInfoForOrderResponse;
import com.example.unbox_user.user.dto.internal.UserInfoForSellingBidResponse;
import com.example.unbox_user.user.entity.User;
import com.example.unbox_user.user.entity.Address;
import com.example.unbox_user.user.repository.UserRepository;
import com.example.unbox_user.user.repository.AddressRepository;
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

    @GetMapping("/{userId}/for-selling-bid")
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

    private final AddressRepository addressRepository;

    @GetMapping("/{userId}/for-order")
    public UserInfoForOrderResponse getUserInfoForOrder(@PathVariable("userId") Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Address defaultAddress = addressRepository.findByUserIdAndIsDefaultTrueAndDeletedAtIsNull(userId)
                .orElse(null);

        return UserInfoForOrderResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .receiverName(defaultAddress != null ? defaultAddress.getReceiver_name() : user.getNickname())
                .receiverPhone(user.getPhone()) // 전화번호는 User 정보 사용
                .receiverAddress(defaultAddress != null ? defaultAddress.getAddress() + " " + defaultAddress.getDetailAddress() : null)
                .receiverZipCode(defaultAddress != null ? defaultAddress.getZipCode() : null)
                .build();
    }
}
