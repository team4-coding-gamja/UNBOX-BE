package com.example.unbox_be.domain.admin.service;

import com.example.unbox_be.domain.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.*;
import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.admin.mapper.AdminMapper;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class AdminStaffServiceImpl implements  AdminStaffService {

    private AdminRepository adminRepository;

    // ✅ 관리자(스태프, 검수자) 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<AdminStaffListResponseDto> getAdminStaffPage(String email, int page, int size) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);

        Page<Admin> admins = adminRepository.findByAdminRoleIn(
                List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR),
                pageable
        );

        return admins.map(AdminMapper::toAdminStaffPageResponseDto);
    }

    // ✅ 관리자(매니저) 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<AdminStaffListResponseDto> getAdminManagerPage(String email, int page, int size) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);

        Page<Admin> admins = adminRepository.findByAdminRoleIn(
                List.of(AdminRole.ROLE_MANAGER),
                pageable
        );

        return admins.map(AdminMapper::toAdminStaffPageResponseDto);
    }

    // ✅ 관리자(검수자) 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<AdminStaffListResponseDto> getAdminInspectorPage(String email, int page, int size) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);

        Page<Admin> admins = adminRepository.findByAdminRoleIn(
                List.of(AdminRole.ROLE_INSPECTOR),
                pageable
        );

        return admins.map(AdminMapper::toAdminStaffPageResponseDto);
    }

    // ✅ 특정 관리자(스태프) 상세 조회
    @Override
    public AdminStaffDetailResponseDto getAdminStaffDetail(String email, Long adminId) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        return AdminMapper.toAdminStaffDetailResponseDto(admin);
    }

    // ✅ 특정 관리자(스태프) 정보 수정
    @Override
    @Transactional
    public AdminStaffUpdateResponseDto updateAdminStaff(String email, Long adminId, AdminStaffUpdateRequestDto requestDto) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        admin.updateAdmin(requestDto.getNickname(), requestDto.getPhone());

        return AdminMapper.toAdminStaffUpdateResponseDto(admin);
    }

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
        return AdminMapper.toAdminMeUpdateResponseDto(admin);
    }
}
