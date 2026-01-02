package com.example.unbox_be.domain.admin.staff.service;

import com.example.unbox_be.domain.admin.staff.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.staff.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.admin.staff.dto.response.*;
import org.springframework.data.domain.Page;

public interface AdminStaffService {

    // ✅ 관리자(매니저 + 검수자) 목록 조회
    Page<AdminStaffListResponseDto> getAdminStaffPage(String email, int page, int size);
    // ✅ 관리자(매니저) 목록 조회
    Page<AdminStaffListResponseDto> getAdminManagerPage(String email, int page, int size);
    // ✅ 관리자(검수자) 목록 조회
    Page<AdminStaffListResponseDto> getAdminInspectorPage(String email, int page, int size);
    // ✅ 특정 관리자(스태프) 상세 조회
    AdminStaffDetailResponseDto getAdminStaffDetail(String email, Long adminId);
    // ✅ 특정 관리자(스태프) 정보 수정
    AdminStaffUpdateResponseDto updateAdminStaff(String email, Long adminId, AdminStaffUpdateRequestDto requestDto);
    // ✅ 내 정보 조회
    AdminMeResponseDto getAdminMe(String email);
    // ✅ 내 정보 수정
    AdminMeUpdateResponseDto updateAdminMe(String email, AdminMeUpdateRequestDto adminMeUpdateRequestDto);
}
