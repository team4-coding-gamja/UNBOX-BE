package com.example.unbox_be.domain.admin.user.controller;

import com.example.unbox_be.domain.admin.user.controller.api.AdminUserApi;
import com.example.unbox_be.domain.admin.user.dto.request.AdminUserUpdateRequestDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserListResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserUpdateResponseDto;
import com.example.unbox_be.domain.admin.user.service.AdminUserService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApi {

    private final AdminUserService adminUserService;

    // ✅ 사용자 목록 조회
    @GetMapping
    public ApiResponse<Page<AdminUserListResponseDto>> getAdminUserPage(
            @RequestParam int page,
            @RequestParam int size) {
        Page<AdminUserListResponseDto> result = adminUserService.getAdminUserPage(page, size);
        return ApiResponse.success(result);
    }

    // ✅ 사용자 상세 정보 조회
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailResponseDto> getAdminUserDetail(
            @PathVariable Long userId) {
        AdminUserDetailResponseDto result = adminUserService.getAdminUserDetail(userId);
        return ApiResponse.success(result);
    }

    // ✅ 사용자 상세 정보 수정
    @PatchMapping("/{userId}")
    public ApiResponse<AdminUserUpdateResponseDto> updateAdminUser(
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserUpdateRequestDto requestDto) {
        AdminUserUpdateResponseDto result = adminUserService.updateAdminUser(userId, requestDto);
        return ApiResponse.success(result);
    }

    // ✅ 사용자 상세 정보 삭제
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteAdminUser(
            @PathVariable Long userId) {
        adminUserService.deleteAdminUser(userId);
        return ApiResponse.success(null);
    }
}
