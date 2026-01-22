package com.example.unbox_user.user.controller;

import com.example.unbox_user.user.controller.api.AdminUserApi;
import com.example.unbox_user.user.dto.request.AdminUserUpdateRequestDto;
import com.example.unbox_user.user.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_user.user.dto.response.AdminUserListResponseDto;
import com.example.unbox_user.user.dto.response.AdminUserUpdateResponseDto;
import com.example.unbox_user.user.service.AdminUserService;
import com.example.unbox_common.pagination.PageSizeLimiter;
import com.example.unbox_common.response.CustomApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApi {

    private final AdminUserService adminUserService;

    // ✅ 사용자 목록 조회
    @GetMapping
    public CustomApiResponse<Page<AdminUserListResponseDto>> getAdminUserPage(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminUserListResponseDto> result = adminUserService.getAdminUserPage(limited);
        return CustomApiResponse.success(result);
    }

    // ✅ 사용자 목록 조회(삭제 포함)
    @GetMapping("/include-deleted")
    public CustomApiResponse<Page<AdminUserListResponseDto>> getAdminUserPageIncludeDeleted(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminUserListResponseDto> result = adminUserService.getAdminUserPageIncludeDeleted(limited);
        return CustomApiResponse.success(result);
    }

    // ✅ 사용자 상세 정보 조회
    @GetMapping("/{userId}")
    public CustomApiResponse<AdminUserDetailResponseDto> getAdminUserDetail(
            @PathVariable Long userId) {
        AdminUserDetailResponseDto result = adminUserService.getAdminUserDetail(userId);
        return CustomApiResponse.success(result);
    }

    // ✅ 사용자 상세 정보 수정
    @PatchMapping("/{userId}")
    public CustomApiResponse<AdminUserUpdateResponseDto> updateAdminUser(
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserUpdateRequestDto requestDto) {
        AdminUserUpdateResponseDto result = adminUserService.updateAdminUser(userId, requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 사용자 삭제
    @DeleteMapping("/{userId}")
    public CustomApiResponse<Void> deleteAdminUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal String deletedBy) {
        adminUserService.deleteAdminUser(userId, deletedBy);
        return CustomApiResponse.success(null);
    }
}
