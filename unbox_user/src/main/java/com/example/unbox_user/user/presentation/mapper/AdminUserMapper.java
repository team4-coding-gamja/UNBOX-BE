package com.example.unbox_user.user.presentation.mapper;

import com.example.unbox_user.user.presentation.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_user.user.presentation.dto.response.AdminUserListResponseDto;
import com.example.unbox_user.user.presentation.dto.response.AdminUserUpdateResponseDto;
import com.example.unbox_user.user.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminUserMapper {

    AdminUserListResponseDto toAdminUserListResponseDto(User user);

    AdminUserDetailResponseDto toAdminUserDetailResponseDto(User user);

    AdminUserUpdateResponseDto toAdminUserUpdateResponseDto(User user);
}