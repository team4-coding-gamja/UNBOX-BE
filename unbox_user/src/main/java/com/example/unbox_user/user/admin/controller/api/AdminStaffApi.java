package com.example.unbox_user.user.admin.controller.api;

import com.example.unbox_user.user.admin.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_user.user.admin.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_user.user.admin.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[관리자] 스태프 관리", description = "관리자용 스태프 관리 API")
@RequestMapping("/api/admin/staff")
public interface AdminStaffApi {

    // ✅ 관리자 정보 목록 조회(매니저 + 검수자)
    @Operation(summary = "관리자 목록 조회", description = "관리자(매니저 + 검수자) 목록을 조회합니다. (삭제된 관리자 제외)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @GetMapping
    CustomApiResponse<Page<AdminStaffListResponseDto>> getAdminStaffPage(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    // ✅ 관리자 정보 목록 조회(매니저)
    @Operation(summary = "매니저 목록 조회", description = "관리자(매니저) 목록을 조회합니다. (삭제된 관리자 제외)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @GetMapping("/managers")
    CustomApiResponse<Page<AdminStaffListResponseDto>> getAdminManagerPage(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    // ✅ 관리자 정보 목록 조회(검수자)
    @Operation(summary = "검수자 목록 조회", description = "관리자(검수자) 목록을 조회합니다. (삭제된 관리자 제외)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @GetMapping("/inspectors")
    CustomApiResponse<Page<AdminStaffListResponseDto>> getAdminInspectorPage(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    // ✅ 특정 관리자(스태프) 상세 조회
    @Operation(summary = "관리자 상세 조회", description = "특정 관리자(스태프) 정보를 상세 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{adminId}")
    CustomApiResponse<AdminStaffDetailResponseDto> getAdminStaffDetail(
            @Parameter(description = "관리자 ID", required = true, example = "1")
            @PathVariable Long adminId
    );

    // ✅ 특정 관리자(스태프) 정보 수정
    @Operation(summary = "관리자 정보 수정", description = "특정 관리자(스태프) 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음", content = @Content)
    })
    @PatchMapping("/{adminId}")
    CustomApiResponse<AdminStaffUpdateResponseDto> updateAdminStaff(
            @Parameter(description = "관리자 ID", required = true, example = "1")
            @PathVariable Long adminId,

            @RequestBody @Valid AdminStaffUpdateRequestDto requestDto
    );

    // ✅ 특정 관리자(스태프) 삭제
    @Operation(summary = "관리자 삭제", description = "특정 관리자(스태프)를 삭제(Soft Delete)합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{adminId}")
    CustomApiResponse<Void> deleteAdmin(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "관리자 ID", required = true, example = "1")
            @PathVariable Long adminId
    );

    // ✅ 내 정보 조회
    @Operation(summary = "내 정보 조회", description = "로그인한 관리자 본인의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/me")
    CustomApiResponse<AdminMeResponseDto> getAdminMe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    // ✅ 내 정보 수정
    @Operation(summary = "내 정보 수정", description = "로그인한 관리자 본인의 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음", content = @Content)
    })
    @PatchMapping("/me")
    CustomApiResponse<AdminMeUpdateResponseDto> updateAdminMe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid AdminMeUpdateRequestDto requestDto
    );

    // ✅ 관리자 목록 조회(매니저 + 검수자) - 삭제 포함(Soft Delete 미적용)
    @Operation(summary = "관리자 목록 조회(삭제 포함)", description = "관리자(매니저 + 검수자) 목록을 조회합니다. (삭제된 관리자 포함)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @GetMapping("/include-deleted")
    CustomApiResponse<Page<AdminStaffListResponseDto>> getAdminStaffPageIncludeDeleted(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );
}