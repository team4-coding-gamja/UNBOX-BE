package com.example.unbox_be.domain.user.admin.service;

import com.example.unbox_be.domain.user.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.user.admin.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.user.admin.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminStaffService {

    // ✅ 관리자(매니저 + 검수자) 목록 조회
    Page<AdminStaffListResponseDto> getAdminStaffPage(Pageable pageable);
    // ✅ 관리자(매니저) 목록 조회
    Page<AdminStaffListResponseDto> getAdminManagerPage(Pageable pageable);
    // ✅ 관리자(검수자) 목록 조회
    Page<AdminStaffListResponseDto> getAdminInspectorPage(Pageable pageable);
    // ✅ 특정 관리자(스태프) 상세 조회
    AdminStaffDetailResponseDto getAdminStaffDetail(Long targetAdminId);
    // ✅ 특정 관리자(스태프) 정보 수정
    AdminStaffUpdateResponseDto updateAdminStaff(Long targetAdminId, AdminStaffUpdateRequestDto requestDto);
    // ✅ 관리자(스태프) 삭제
    void deleteAdmin(Long adminId, String deletedBy);
    // ✅ 내 정보 조회
    AdminMeResponseDto getAdminMe(Long adminId);
    // ✅ 내 정보 수정
    AdminMeUpdateResponseDto updateAdminMe(Long adminId, AdminMeUpdateRequestDto adminMeUpdateRequestDto);
    // ✅ 삭제된 관리자까지 포함해서(Soft Delete 미적용) 관리자 목록 조회
    Page<AdminStaffListResponseDto> getAdminStaffPageIncludeDeleted(Pageable pageable);
}
