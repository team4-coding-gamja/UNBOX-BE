package com.example.unbox_be.domain.user.admin.mapper;

import com.example.unbox_be.domain.user.admin.dto.response.*;
import com.example.unbox_be.domain.user.admin.entity.Admin;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AdminStaffMapper {

    // ✅ 목록 DTO
    AdminStaffListResponseDto toAdminStaffListResponseDto(Admin admin);

    // ✅ 상세 DTO
    AdminStaffDetailResponseDto toAdminStaffDetailResponseDto(Admin admin);

    // ✅ 스태프 수정 응답 DTO
    AdminStaffUpdateResponseDto toAdminStaffUpdateResponseDto(Admin admin);

    // ✅ 내 정보 조회 DTO
    AdminMeResponseDto toAdminMeResponseDto(Admin admin);

    // ✅ 내 정보 수정 응답 DTO
    AdminMeUpdateResponseDto toAdminMeUpdateResponseDto(Admin admin);
}
