package com.example.unbox_be.domain.admin.controller;

import com.example.unbox_be.domain.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.*;
import com.example.unbox_be.domain.admin.service.AdminStaffService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

//    // ✅ 관리자 정보 조회
//    @GetMapping
//    public ApiResponse<List<AdminStaffListResponseDto>> getAdminStaffList(
//        @AuthenticationPrincipal CustomUserDetails userDetails) {
//        List<AdminStaffListResponseDto> result = adminStaffService.getAdminStaffList();
//        return ApiResponse.success(result);
//    }
//
//    // ✅ 관리자 정보 수정
//    @GetMapping("/{adminId}")
//    public ApiResponse<AdminStaffDetailResponseDto> getAdminStaffDetail(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @PathVariable Long adminId) {
//        AdminStaffDetailResponseDto result = adminStaffService.getAdminStaffDetail(adminId);
//        return ApiResponse.success(result);
//    }
//
//    // ✅ 관리자 정보 삭제
//    @PatchMapping("/{adminId}")
//    public ApiResponse<AdminStaffUpdateResponseDto> updateAdminStaff(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @PathVariable Long adminId,
//            @RequestBody @Valid AdminStaffUpdateRequestDto requestDto) {
//        AdminStaffUpdateResponseDto result = adminStaffService.updateAdminStaff(adminId, requestDto);
//        return ApiResponse.success(result);
//    }

    // ✅ 관리자 내 정보 조회
    @GetMapping("/me")
    public ApiResponse<AdminMeResponseDto> getAdminMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminMeResponseDto result = adminStaffService.getAdminMe(userDetails.getUsername());
        return ApiResponse.success(result);
    }

    // ✅ 관리자 내 정보 수정
    @PatchMapping("/me")
    public ApiResponse<AdminMeUpdateResponseDto> updateUserMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AdminMeUpdateRequestDto adminMeUpdateRequestDto) {
        AdminMeUpdateResponseDto result = adminStaffService.updateAdminMe(userDetails.getUsername(), adminMeUpdateRequestDto);
        return ApiResponse.success(result);
    }
}
