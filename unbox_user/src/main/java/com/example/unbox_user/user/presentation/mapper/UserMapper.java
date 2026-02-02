package com.example.unbox_user.user.presentation.mapper;

import com.example.unbox_user.user.presentation.dto.response.UserMeResponseDto;
import com.example.unbox_user.user.presentation.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_user.user.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserMeResponseDto toUserMeResponseDto(User user);

    UserMeUpdateResponseDto toUserMeUpdateResponseDto(User user);
}