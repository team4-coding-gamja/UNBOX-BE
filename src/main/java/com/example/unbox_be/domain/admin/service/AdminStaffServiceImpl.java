package com.example.unbox_be.domain.admin.service;

import com.example.unbox_be.domain.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.*;
import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.domain.admin.mapper.AdminStaffMapper;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class AdminStaffServiceImpl implements  AdminStaffService {

    private AdminRepository adminRepository;
    private AdminStaffMapper adminStaffMapper;

    // ✅ 관리자(스태프, 검수자) 목록 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MASTER')")
    public Page<AdminStaffListResponseDto> getAdminStaffPage(Pageable pageable) {
        Page<Admin> admins = adminRepository.findAllByAdminRoleInAndDeletedAtIsNull(
                List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR),
                pageable
        );
        return admins.map(adminStaffMapper::toAdminStaffListResponseDto);
    }

    // ✅ 관리자(매니저) 목록 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MASTER')")
    public Page<AdminStaffListResponseDto> getAdminManagerPage(Pageable pageable) {
        Page<Admin> admins = adminRepository.findAllByAdminRoleInAndDeletedAtIsNull(
                List.of(AdminRole.ROLE_MANAGER),
                pageable
        );
        return admins.map(adminStaffMapper::toAdminStaffListResponseDto);
    }

    // ✅ 관리자(검수자) 목록 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MASTER')")
    public Page<AdminStaffListResponseDto> getAdminInspectorPage(Pageable pageable) {
        Page<Admin> admins = adminRepository.findAllByAdminRoleInAndDeletedAtIsNull(
                List.of(AdminRole.ROLE_INSPECTOR),
                pageable
        );
        return admins.map(adminStaffMapper::toAdminStaffListResponseDto);
    }

    // ✅ 특정 관리자(스태프) 상세 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MASTER')")
    public AdminStaffDetailResponseDto getAdminStaffDetail(Long targetAdminId) {
        Admin admin = adminRepository.findByIdAndDeletedAtIsNull(targetAdminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        return adminStaffMapper.toAdminStaffDetailResponseDto(admin);
    }

    // ✅ 특정 관리자(스태프) 정보 수정
    @Override
    @Transactional
    @PreAuthorize("hasRole('MASTER')")
    public AdminStaffUpdateResponseDto updateAdminStaff(Long targetAdminId, AdminStaffUpdateRequestDto requestDto) {
        Admin admin = adminRepository.findByIdAndDeletedAtIsNull(targetAdminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        admin.updateAdmin(requestDto.getNickname(), requestDto.getPhone());
        return adminStaffMapper.toAdminStaffUpdateResponseDto(admin);
    }

    // ✅ 특정 관리자(스태프) 삭제
    @Override
    @Transactional
    @PreAuthorize("hasRole('MASTER')")
    public void deleteAdmin(Long adminId, String deletedBy) {

        Admin admin = adminRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        admin.softDelete(deletedBy);
    }

    // ✅ 내 관리자 정보 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MASTER','MANAGER','INSPECTOR')")
    public AdminMeResponseDto getAdminMe(Long adminId) {
        Admin admin = adminRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        return adminStaffMapper.toAdminMeResponseDto(admin);
    }

    // ✅ 내 관리자 정보 수정
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER','INSPECTOR')")
    public AdminMeUpdateResponseDto updateAdminMe(Long adminId, AdminMeUpdateRequestDto adminUpdateRequestDto) {
        Admin admin = adminRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        admin.updateAdmin(
                adminUpdateRequestDto.getNickname(),
                adminUpdateRequestDto.getPhone()
        );
        return adminStaffMapper.toAdminMeUpdateResponseDto(admin);
    }

    // ✅ 관리자(스태프, 검수자) 목록 조회(삭제 포함)
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MASTER')")
    public Page<AdminStaffListResponseDto> getAdminStaffPageIncludeDeleted(Pageable pageable) {

        Page<Admin> admins = adminRepository.findByAdminRoleInAndDeletedAtIsNull(
                List.of(AdminRole.ROLE_MANAGER, AdminRole.ROLE_INSPECTOR),
                pageable
                );
        return admins.map(adminStaffMapper::toAdminStaffListResponseDto);
    }

}
