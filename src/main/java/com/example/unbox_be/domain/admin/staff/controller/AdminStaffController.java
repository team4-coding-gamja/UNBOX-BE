package com.example.unbox_be.domain.admin.staff.controller;

import com.example.unbox_be.domain.admin.staff.controller.api.AdminStaffApi;
import com.example.unbox_be.domain.admin.staff.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.staff.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.admin.staff.service.AdminStaffService;
import com.example.unbox_be.domain.admin.staff.dto.response.*;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController implements AdminStaffApi {

    private final AdminStaffService adminStaffService;

    // ✅ 관리자 정보 목록 조회(매니저 + 검수자)
    @GetMapping
    public ApiResponse<Page<AdminStaffListResponseDto>> getAdminStaffPage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam int page,
        @RequestParam int size) {
        Page<AdminStaffListResponseDto> result = adminStaffService.getAdminStaffPage(userDetails.getUsername(), page, size);
        return ApiResponse.success(result);
    }

    // ✅ 관리자 정보 목록 조회(매니저)
    @GetMapping("/managers")
    public ApiResponse<Page<AdminStaffListResponseDto>> getAdminManagerPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int page,
            @RequestParam int size) {
        Page<AdminStaffListResponseDto> result = adminStaffService.getAdminManagerPage(userDetails.getUsername(), page, size);
        return ApiResponse.success(result);
    }

    // ✅ 관리자 정보 목록 조회(검수자)
    @GetMapping("/inspectors")
    public ApiResponse<Page<AdminStaffListResponseDto>> getAdminInspectorPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int page,
            @RequestParam int size) {
        Page<AdminStaffListResponseDto> result = adminStaffService.getAdminInspectorPage(userDetails.getUsername(), page, size);
        return ApiResponse.success(result);
    }

    // ✅ 특정 관리자(스태프) 상세 조회
    @GetMapping("/{adminId}")
    public ApiResponse<AdminStaffDetailResponseDto> getAdminStaffDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long adminId) {
        AdminStaffDetailResponseDto result = adminStaffService.getAdminStaffDetail(userDetails.getUsername(), adminId);
        return ApiResponse.success(result);
    }

    // ✅ 특정 관리자(스태프) 상세 수정
    @PatchMapping("/{adminId}")
    public ApiResponse<AdminStaffUpdateResponseDto> updateAdminStaff(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long adminId,
            @RequestBody @Valid AdminStaffUpdateRequestDto requestDto) {
        AdminStaffUpdateResponseDto result = adminStaffService.updateAdminStaff(userDetails.getUsername(), adminId, requestDto);
        return ApiResponse.success(result);
    }

    // ✅ 관리자 내 정보 조회
    @GetMapping("/me")
    public ApiResponse<AdminMeResponseDto> getAdminMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminMeResponseDto result = adminStaffService.getAdminMe(userDetails.getUsername());
        return ApiResponse.success(result);
    }

    // ✅ 관리자 내 정보 수정
    @PatchMapping("/me")
    public ApiResponse<AdminMeUpdateResponseDto> updateAdminMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AdminMeUpdateRequestDto requestDto) {
        AdminMeUpdateResponseDto result = adminStaffService.updateAdminMe(userDetails.getUsername(), requestDto);
        return ApiResponse.success(result);
    }
}
