package com.example.unbox_be.domain.user.controller;

import com.example.unbox_be.domain.user.controller.api.UserApi;
import com.example.unbox_be.domain.user.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserMeResponseDto;
import com.example.unbox_be.domain.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_be.domain.user.service.UserService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    // 회원 정보 조회
    @GetMapping("/me")
    public ApiResponse<UserMeResponseDto> getUserMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserMeResponseDto userMeResponseDto = userService.getUserMe(userDetails.getUsername());
        return ApiResponse.success(userMeResponseDto);
    }

    // 회원 정보 수정
    @PatchMapping("/me")
    public ApiResponse<UserMeUpdateResponseDto> updateUserMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserMeUpdateRequestDto userMeUpdateRequestDto) {
        UserMeUpdateResponseDto userMeUpdateResponseDto = userService.updateUserMe(userDetails.getUsername(), userMeUpdateRequestDto);
        return ApiResponse.success(userMeUpdateResponseDto);
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ApiResponse<String> deleteUserMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteUserMe(userDetails.getUsername());
        return ApiResponse.successWithNoData();
    }
}
