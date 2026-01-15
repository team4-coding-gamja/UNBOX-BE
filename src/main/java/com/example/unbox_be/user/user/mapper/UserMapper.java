package com.example.unbox_be.user.user.mapper;

import com.example.unbox_be.user.user.dto.response.UserMeResponseDto;
import com.example.unbox_be.user.user.dto.response.UserMeUpdateResponseDto;
import com.example.unbox_be.user.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserMeResponseDto toUserMeResponseDto(User user);

    UserMeUpdateResponseDto toUserMeUpdateResponseDto(User user);
}