package com.example.unbox_order.order.presentation.controller.api;

import com.example.unbox_order.order.presentation.dto.request.inspection.InspectionCreateRequestDto;
import com.example.unbox_order.order.presentation.dto.request.inspection.InspectionResultRequestDto;
import com.example.unbox_order.order.presentation.dto.response.inspection.InspectionResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "[관리자] 검수 관리", description = "관리자 검수 API")
public interface AdminInspectionApi {

    @Operation(summary = "검수 시작", description = "주문 상태를 '검수 중'으로 변경하고 검수 레코드를 생성합니다.")
    CustomApiResponse<InspectionResponseDto> startInspection(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody InspectionCreateRequestDto requestDto);

    @Operation(summary = "검수 합격 처리", description = "검수 결과를 합격으로 처리하고 주문 상태를 변경합니다.")
    CustomApiResponse<InspectionResponseDto> passInspection(
            @Parameter(description = "검수 ID", required = true) UUID inspectionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody InspectionResultRequestDto requestDto);

    @Operation(summary = "검수 불합격 처리", description = "검수 결과를 불합격으로 처리하고 주문 상태를 변경합니다.")
    CustomApiResponse<InspectionResponseDto> failInspection(
            @Parameter(description = "검수 ID", required = true) UUID inspectionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody InspectionResultRequestDto requestDto);

    @Operation(summary = "주문별 검수 조회", description = "특정 주문의 검수 이력을 조회합니다.")
    CustomApiResponse<InspectionResponseDto> getInspection(
            @Parameter(description = "주문 ID", required = true) UUID orderId);
}
