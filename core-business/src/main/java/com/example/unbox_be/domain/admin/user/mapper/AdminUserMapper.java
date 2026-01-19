package com.example.unbox_be.domain.admin.user.mapper;

import com.example.unbox_be.domain.admin.user.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserListResponseDto;
import com.example.unbox_be.domain.admin.user.dto.response.AdminUserUpdateResponseDto;
import com.example.unbox_be.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminUserMapper {

    AdminUserListResponseDto toAdminUserListResponseDto(User user);

    AdminUserDetailResponseDto toAdminUserDetailResponseDto(User user);

    AdminUserUpdateResponseDto toAdminUserUpdateResponseDto(User user);
}