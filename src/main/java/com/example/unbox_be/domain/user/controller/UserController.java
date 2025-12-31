package com.example.unbox_be.domain.user.controller;

import com.example.unbox_be.domain.user.controller.api.UserApi;
import com.example.unbox_be.domain.user.dto.request.UserUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserResponseDto;
import com.example.unbox_be.domain.user.service.UserService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
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
    public ApiResponse<UserResponseDto> getUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDto userResponseDto = userService.getUserByEmail(userDetails.getUsername());
        return ApiResponse.success(userResponseDto);
    }

    // 회원 정보 수정
    @PatchMapping("/me")
    public ApiResponse<UserResponseDto> updateUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                   @RequestBody UserUpdateRequestDto userUpdateRequestDto) {
        userService.updateUser(userDetails.getUsername(), userUpdateRequestDto);
        UserResponseDto userResponseDto = userService.getUserByEmail(userDetails.getUsername());
        return ApiResponse.success(userResponseDto);
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ApiResponse<String> deleteUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteUser(userDetails.getUsername());
        return ApiResponse.successWithNoData();
    }
}
