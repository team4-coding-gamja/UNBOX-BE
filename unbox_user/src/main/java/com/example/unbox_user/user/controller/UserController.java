package com.example.unbox_user.user.controller;

import com.example.unbox_user.user.controller.api.UserApi;
import com.example.unbox_user.user.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_user.user.dto.response.UserMeResponseDto;
import com.example.unbox_user.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_user.user.service.UserService;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    // ✅ 버전 정보 (Blue/Green 배포 테스트용)
    @GetMapping("/version")
    public CustomApiResponse<String> getVersion() {
        return CustomApiResponse.success("User Service v2.0 - Blue/Green Deployment Test");
    }

    // ✅ 내 정보 조회
    @GetMapping("/me")
    public CustomApiResponse<UserMeResponseDto> getUserMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserMeResponseDto userMeResponseDto = userService.getUserMe(userDetails.getUserId());
        return CustomApiResponse.success(userMeResponseDto);
    }

    // ✅ 내 정보 수정
    @PatchMapping("/me")
    public CustomApiResponse<UserMeUpdateResponseDto> updateUserMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserMeUpdateRequestDto requestDto) {
        UserMeUpdateResponseDto userMeUpdateResponseDto = userService.updateUserMe(userDetails.getUserId(), requestDto);
        return CustomApiResponse.success(userMeUpdateResponseDto);
    }

    // ✅ 회원 탈퇴
    @DeleteMapping("/me")
    public CustomApiResponse<String> deleteUserMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteUserMe(userDetails.getUserId());
        return CustomApiResponse.success(null);
    }
}
