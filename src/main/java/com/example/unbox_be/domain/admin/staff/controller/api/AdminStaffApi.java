package com.example.unbox_be.domain.admin.staff.controller.api;

import com.example.unbox_be.domain.admin.staff.dto.request.AdminMeUpdateRequestDto;
import com.example.unbox_be.domain.admin.staff.dto.request.AdminStaffUpdateRequestDto;
import com.example.unbox_be.domain.admin.staff.dto.response.*;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 스태프", description = "관리자(스태프/검수자) 관리 API")
@RequestMapping("/api/admin/staff")
public interface AdminStaffApi {

    @Operation(
            summary = "관리자 목록 조회(매니저 + 검수자)",
            description = "관리자(매니저 + 검수자) 계정을 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    ApiResponse<Page<AdminStaffListResponseDto>> getAdminStaffPage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "페이지 번호(0부터 시작)", example = "0", required = true)
            @RequestParam int page,

            @Parameter(description = "페이지 크기", example = "10", required = true)
            @RequestParam int size
    );

    @Operation(
            summary = "관리자 목록 조회(매니저만)",
            description = "관리자(매니저) 계정만 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/managers")
    ApiResponse<Page<AdminStaffListResponseDto>> getAdminManagerPage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "페이지 번호(0부터 시작)", example = "0", required = true)
            @RequestParam int page,

            @Parameter(description = "페이지 크기", example = "10", required = true)
            @RequestParam int size
    );

    @Operation(
            summary = "관리자 목록 조회(검수자만)",
            description = "관리자(검수자) 계정만 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/inspectors")
    ApiResponse<Page<AdminStaffListResponseDto>> getAdminInspectorPage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "페이지 번호(0부터 시작)", example = "0", required = true)
            @RequestParam int page,

            @Parameter(description = "페이지 크기", example = "10", required = true)
            @RequestParam int size
    );

    @Operation(
            summary = "관리자 상세 조회",
            description = "adminId에 해당하는 관리자 정보를 상세 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음")
    })
    @GetMapping("/{adminId}")
    ApiResponse<AdminStaffDetailResponseDto> getAdminStaffDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "관리자 ID", required = true, example = "1")
            @PathVariable Long adminId
    );

    @Operation(
            summary = "관리자 정보 수정",
            description = "adminId에 해당하는 관리자 정보를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음")
    })
    @PatchMapping("/{adminId}")
    ApiResponse<AdminStaffUpdateResponseDto> updateAdminStaff(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "관리자 ID", required = true, example = "1")
            @PathVariable Long adminId,

            @RequestBody @Valid AdminStaffUpdateRequestDto requestDto
    );

    @Operation(
            summary = "내 관리자 정보 조회",
            description = "현재 로그인한 관리자(내 계정) 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음")
    })
    @GetMapping("/me")
    ApiResponse<AdminMeResponseDto> getAdminMe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "내 관리자 정보 수정",
            description = "현재 로그인한 관리자(내 계정) 정보를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 정보 수정 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음")
    })
    @PatchMapping("/me")
    ApiResponse<AdminMeUpdateResponseDto> updateAdminMe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid AdminMeUpdateRequestDto requestDto
    );
}