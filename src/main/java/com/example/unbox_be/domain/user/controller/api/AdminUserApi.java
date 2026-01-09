package com.example.unbox_be.domain.user.controller.api;

import com.example.unbox_be.domain.user.dto.request.AdminUserUpdateRequestDto;
import com.example.unbox_be.domain.user.dto.response.AdminUserDetailResponseDto;
import com.example.unbox_be.domain.user.dto.response.AdminUserListResponseDto;
import com.example.unbox_be.domain.user.dto.response.AdminUserUpdateResponseDto;
import com.example.unbox_be.global.response.CustomApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[관리자] 사용자 관리", description = "관리자용 사용자 괸리 API")
public interface AdminUserApi {

    @Operation(
            summary = "사용자 목록 조회",
            description = "관리자가 전체 사용자 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0")),
            @Parameter(name = "size", description = "페이지 크기", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "sort", description = "정렬 (필드명,ASC|DESC)", in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "createdAt,DESC"))
    })
    @GetMapping
    CustomApiResponse<Page<AdminUserListResponseDto>> getAdminUserPage(
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(
            summary = "사용자 목록 조회(삭제 포함)",
            description = "관리자가 전체 사용자 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0")),
            @Parameter(name = "size", description = "페이지 크기", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "sort", description = "정렬 (필드명,ASC|DESC)", in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "createdAt,DESC"))
    })
    @GetMapping("/include-deleted")
    CustomApiResponse<Page<AdminUserListResponseDto>> getAdminUserPageIncludeDeleted(
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(
            summary = "사용자 상세 조회",
            description = "특정 사용자의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}")
    CustomApiResponse<AdminUserDetailResponseDto> getAdminUserDetail(
            @Parameter(description = "사용자 ID (Long)", required = true)
            @PathVariable Long userId
    );

    @Operation(
            summary = "사용자 정보 수정",
            description = "사용자의 닉네임, 전화번호 등 정보를 수정합니다."
    )
    @PatchMapping("/{userId}")
    CustomApiResponse<AdminUserUpdateResponseDto> updateAdminUser(
            @Parameter(description = "사용자 ID (Long)", required = true)
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserUpdateRequestDto requestDto
    );

    @Operation(
            summary = "사용자 삭제",
            description = "관리자가 특정 사용자를 삭제(Soft Delete)합니다."
    )
    @DeleteMapping("/{userId}")
    CustomApiResponse<Void> deleteAdminUser(
            @Parameter(description = "사용자 ID (Long)", required = true)
            @PathVariable Long userId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal String deletedBy
    );
}