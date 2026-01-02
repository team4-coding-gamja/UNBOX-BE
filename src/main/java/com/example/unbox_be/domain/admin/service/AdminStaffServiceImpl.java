package com.example.unbox_be.domain.admin.service;

import com.example.unbox_be.domain.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.AdminMeResponseDto;
import com.example.unbox_be.domain.admin.dto.response.AdminMeUpdateResponseDto;
import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.mapper.AdminMapper;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStaffServiceImpl implements  AdminStaffService {

    private AdminStaffService adminStaffService;
    private AdminRepository adminRepository;
    private AdminMapper adminMapper;

    // 관리자 내 정보 조회 API
    @Transactional(readOnly = true)
    public AdminMeResponseDto getAdminMe(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        return AdminMapper.toAdminMeResponseDto(admin);
    }

    // 관리자 내 정보 수정 API
    @Transactional
    public AdminMeUpdateResponseDto updateAdminMe(String email, AdminMeUpdateRequestDto adminUpdateRequestDto) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        admin.updateAdmin(
                adminUpdateRequestDto.getNickname(),
                adminUpdateRequestDto.getPhone()
        );
        return null;
    }

}
