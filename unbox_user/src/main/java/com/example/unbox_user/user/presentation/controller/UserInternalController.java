package com.example.unbox_user.user.presentation.controller;

import com.example.unbox_user.user.domain.repository.AccountRepository;
import com.example.unbox_user.user.presentation.dto.internal.UserInfoForOrderResponse;
import com.example.unbox_user.user.presentation.dto.internal.UserInfoForSellingBidResponse;
import com.example.unbox_user.user.domain.entity.User;
import com.example.unbox_user.user.domain.entity.Address;
import com.example.unbox_user.user.domain.repository.UserRepository;
import com.example.unbox_user.user.domain.repository.AddressRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "[내부] 유저 관리", description = "내부 시스템용 유저 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class UserInternalController {

    private final UserRepository userRepository;

    @Operation(summary = "판매 입찰용 유저 정보 조회", description = "판매 입찰 시 필요한 유저 정보를 조회합니다.")
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

    @Operation(summary = "주문용 유저 정보 조회", description = "주문 시 필요한 유저 및 배송지 정보를 조회합니다.")
    @GetMapping("/{userId}/for-order")
    public UserInfoForOrderResponse getUserInfoForOrder(@PathVariable("userId") Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Address defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(userId)
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
    private final AccountRepository accountRepository;

    @Operation(summary = "기본 계좌 존재 여부 확인", description = "유저가 기본 정산 계좌를 가지고 있는지 확인합니다.")
    @GetMapping("/{userId}/has-default-account")
    public boolean hasDefaultAccount(@PathVariable("userId") Long userId) {
        return accountRepository.existsByUserIdAndIsDefaultTrue(userId);
    }
}
