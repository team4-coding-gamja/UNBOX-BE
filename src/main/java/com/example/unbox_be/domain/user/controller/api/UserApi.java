package com.example.unbox_be.domain.user.controller.api;

import com.example.unbox_be.domain.user.dto.request.UserUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserResponseDto;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 관리", description = "회원 정보 조회 / 수정 / 탈퇴 API")
@RequestMapping("/api/users")
public interface UserApi {

    // ================= 회원 조회 =================
    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인된 사용자의 회원 정보를 조회합니다."
    )
    @ApiResponses({
    })
    @GetMapping("/me")
    ApiResponse<UserResponseDto> getUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    // ================= 회원 수정 =================
    @Operation(
            summary = "내 정보 수정",
            description = "현재 로그인된 사용자의 닉네임과 전화번호를 수정합니다."
    )
    @ApiResponses({
    })
    @PatchMapping("/me")
    ApiResponse<UserResponseDto> updateUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "수정할 회원 정보", required = true)
            @RequestBody UserUpdateRequestDto userUpdateRequestDto
    );

    // ================= 회원 탈퇴 =================
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인된 사용자를 탈퇴 처리합니다."
    )
    @ApiResponses({
    })
    @DeleteMapping("/me")
    ApiResponse<String> deleteUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
