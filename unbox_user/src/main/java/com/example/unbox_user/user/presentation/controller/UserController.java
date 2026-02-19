package com.example.unbox_user.user.presentation.controller;

import com.example.unbox_user.user.presentation.controller.api.UserApi;
import com.example.unbox_user.user.presentation.dto.request.UserMeUpdateRequestDto;
import com.example.unbox_user.user.presentation.dto.response.UserMeResponseDto;
import com.example.unbox_user.user.presentation.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_user.user.application.service.UserService;
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
        return CustomApiResponse.success("User Service v4.0 - Blue/Green Manual Rollback Test");
    }

    // ✅ Health Test 엔드포인트 (시나리오 2: 느린 응답 - 수동 롤백 테스트)
    @GetMapping("/health-test")
    public CustomApiResponse<String> healthTest() throws InterruptedException {
        // 의도적으로 2초 지연 (문제 있는 Green 버전)
        Thread.sleep(2000);
        return CustomApiResponse.success("User Service v4.0 - Slow Response (2s delay) - Manual Test");
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
