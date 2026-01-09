package com.example.unbox_be.domain.admin.controller;

import com.example.unbox_be.domain.admin.controller.api.AdminStaffApi;
import com.example.unbox_be.domain.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.*;
import com.example.unbox_be.domain.admin.service.AdminStaffService;
import com.example.unbox_be.global.pagination.PageSizeLimiter;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController implements AdminStaffApi {

    private final AdminStaffService adminStaffService;

    // ✅ 관리자 정보 목록 조회(매니저 + 검수자)
    @GetMapping
    public CustomApiResponse<Page<AdminStaffListResponseDto>> getAdminStaffPage(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminStaffListResponseDto> result = adminStaffService.getAdminStaffPage(limited);
        return CustomApiResponse.success(result);
    }

    // ✅ 관리자 정보 목록 조회(매니저)
    @GetMapping("/managers")
    public CustomApiResponse<Page<AdminStaffListResponseDto>> getAdminManagerPage(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminStaffListResponseDto> result = adminStaffService.getAdminManagerPage(limited);
        return CustomApiResponse.success(result);
    }

    // ✅ 관리자 정보 목록 조회(검수자)
    @GetMapping("/inspectors")
    public CustomApiResponse<Page<AdminStaffListResponseDto>> getAdminInspectorPage(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminStaffListResponseDto> result = adminStaffService.getAdminInspectorPage(limited);
        return CustomApiResponse.success(result);
    }

    // ✅ 특정 관리자(스태프) 상세 조회
    @GetMapping("/{adminId}")
    public CustomApiResponse<AdminStaffDetailResponseDto> getAdminStaffDetail(
            @PathVariable Long adminId) {
        AdminStaffDetailResponseDto result = adminStaffService.getAdminStaffDetail(adminId);
        return CustomApiResponse.success(result);
    }

    // ✅ 특정 관리자(스태프) 상세 수정
    @PatchMapping("/{adminId}")
    public CustomApiResponse<AdminStaffUpdateResponseDto> updateAdminStaff(
            @PathVariable Long adminId,
            @RequestBody @Valid AdminStaffUpdateRequestDto requestDto) {
        AdminStaffUpdateResponseDto result = adminStaffService.updateAdminStaff(adminId, requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 특정 관리자(스태프) 삭제
    @DeleteMapping("/{adminId}")
    public CustomApiResponse<Void> deleteAdmin(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long adminId) {
        String deletedBy = userDetails.getUsername();
        adminStaffService.deleteAdmin(adminId, deletedBy);
        return CustomApiResponse.success(null);
    }

    // ✅ 관리자 내 정보 조회
    @GetMapping("/me")
    public CustomApiResponse<AdminMeResponseDto> getAdminMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminMeResponseDto result = adminStaffService.getAdminMe(userDetails.getAdminId());
        return CustomApiResponse.success(result);
    }

    // ✅ 관리자 내 정보 수정
    @PatchMapping("/me")
    public CustomApiResponse<AdminMeUpdateResponseDto> updateAdminMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AdminMeUpdateRequestDto requestDto) {
        AdminMeUpdateResponseDto result = adminStaffService.updateAdminMe(userDetails.getAdminId(), requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 관리자 목록 조회(매니저 + 검수자) - 삭제 포함(Soft Delete 미적용)
    @GetMapping("/include-deleted")
    public CustomApiResponse<Page<AdminStaffListResponseDto>> getAdminStaffPageIncludeDeleted(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminStaffListResponseDto> result = adminStaffService.getAdminStaffPageIncludeDeleted(limited);
        return CustomApiResponse.success(result);
    }
}
