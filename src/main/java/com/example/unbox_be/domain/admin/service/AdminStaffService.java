package com.example.unbox_be.domain.admin.service;

import com.example.unbox_be.domain.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.AdminMeResponseDto;
import com.example.unbox_be.domain.admin.dto.response.AdminMeUpdateResponseDto;

public interface AdminStaffService {

    // 관리자 내 정보 조회 API
    AdminMeResponseDto getAdminMe(String email);

    // 관리자 내 정보 수정
    AdminMeUpdateResponseDto updateAdminMe(String email, AdminMeUpdateRequestDto adminMeUpdateRequestDto);
}
