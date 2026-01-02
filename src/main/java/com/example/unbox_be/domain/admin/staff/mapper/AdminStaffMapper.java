package com.example.unbox_be.domain.admin.staff.mapper;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.staff.dto.response.*;

public class AdminStaffMapper {

    public static AdminMeResponseDto toAdminMeResponseDto(Admin admin) {
        return AdminMeResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .adminStatus(admin.getAdminStatus())
                .build();
    }

    public static AdminMeUpdateResponseDto toAdminMeUpdateResponseDto(Admin admin){
        return AdminMeUpdateResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .build();
    }

    public static AdminStaffListResponseDto toAdminStaffPageResponseDto(Admin admin) {
        return AdminStaffListResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .adminStatus(admin.getAdminStatus())
                .build();
    }

    public static AdminStaffDetailResponseDto toAdminStaffDetailResponseDto(Admin admin) {
        return AdminStaffDetailResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .adminStatus(admin.getAdminStatus())
                .build();
    }

    public static AdminStaffUpdateResponseDto toAdminStaffUpdateResponseDto(Admin admin) {
        return AdminStaffUpdateResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .adminStatus(admin.getAdminStatus())
                .build();
    }
}
