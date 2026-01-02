package com.example.unbox_be.domain.admin.staff.service;

import com.example.unbox_be.domain.admin.staff.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.staff.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.admin.staff.dto.response.*;
import com.example.unbox_be.domain.admin.staff.mapper.AdminStaffMapper;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('MASTER')")
    public Page<AdminStaffListResponseDto> getAdminStaffPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Admin> admins = adminRepository.findByAdminRoleIn(
                List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR),
                pageable
        );
        return admins.map(AdminStaffMapper::toAdminStaffPageResponseDto);
    }

    // ✅ 관리자(매니저) 목록 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MASTER')")
    public Page<AdminStaffListResponseDto> getAdminManagerPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Admin> admins = adminRepository.findByAdminRoleIn(
                List.of(AdminRole.ROLE_MANAGER),
                pageable
        );
        return admins.map(AdminStaffMapper::toAdminStaffPageResponseDto);
    }

    // ✅ 관리자(검수자) 목록 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MASTER')")
    public Page<AdminStaffListResponseDto> getAdminInspectorPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Admin> admins = adminRepository.findByAdminRoleIn(
                List.of(AdminRole.ROLE_INSPECTOR),
                pageable
        );
        return admins.map(AdminStaffMapper::toAdminStaffPageResponseDto);
    }

    // ✅ 특정 관리자(스태프) 상세 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MASTER')")
    public AdminStaffDetailResponseDto getAdminStaffDetail(Long targetAdminId) {
        Admin admin = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        return AdminStaffMapper.toAdminStaffDetailResponseDto(admin);
    }

    // ✅ 특정 관리자(스태프) 정보 수정
    @Override
    @Transactional
    @PreAuthorize("hasRole('MASTER')")
    public AdminStaffUpdateResponseDto updateAdminStaff(Long targetAdminId, AdminStaffUpdateRequestDto requestDto) {
        Admin admin = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        admin.updateAdmin(requestDto.getNickname(), requestDto.getPhone());
        return AdminStaffMapper.toAdminStaffUpdateResponseDto(admin);
    }

    // 관리자 내 정보 조회 API
    @Override
    @Transactional(readOnly = true)
    public AdminMeResponseDto getAdminMe(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        return AdminStaffMapper.toAdminMeResponseDto(admin);
    }

    // 관리자 내 정보 수정 API
    @Override
    @Transactional
    public AdminMeUpdateResponseDto updateAdminMe(Long adminId, AdminMeUpdateRequestDto adminUpdateRequestDto) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        admin.updateAdmin(
                adminUpdateRequestDto.getNickname(),
                adminUpdateRequestDto.getPhone()
        );
        return AdminStaffMapper.toAdminMeUpdateResponseDto(admin);
    }
}
