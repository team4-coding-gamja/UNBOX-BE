package com.example.unbox_be.domain.admin.user.service;

import com.example.unbox_be.domain.admin.user.dto.request.AdminUserUpdateRequestDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserListResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserUpdateResponseDto;
import org.springframework.data.domain.Page;

public interface AdminUserService {

    // ✅ 사용자 목록 조회
    Page<AdminUserListResponseDto> getAdminUserPage(String email, int page, int size);
    // ✅ 사용자 상세 정보 조회
    AdminUserDetailResponseDto getAdminUserDetail(String email, Long userId);
    // ✅ 사용자 상세 정보 수정
    AdminUserUpdateResponseDto updateAdminUser(String email, Long adminId, AdminUserUpdateRequestDto requestDto);
    // ✅ 사용자 상세 정보 삭제
    void deleteAdminUser(String email, Long adminId);
}
