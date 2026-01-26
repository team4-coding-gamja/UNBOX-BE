package com.example.unbox_order.order.presentation.controller;

import com.example.unbox_order.order.application.service.InspectionService;
import com.example.unbox_order.order.presentation.controller.api.AdminInspectionApi;
import com.example.unbox_order.order.presentation.dto.request.inspection.InspectionCreateRequestDto;
import com.example.unbox_order.order.presentation.dto.request.inspection.InspectionResultRequestDto;
import com.example.unbox_order.order.presentation.dto.response.inspection.InspectionResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inspections")
public class AdminInspectionController implements AdminInspectionApi {

    private final InspectionService inspectionService;

    // ✅ 검수 시작
    @Override
    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('MASTER', 'INSPECTOR')")
    public CustomApiResponse<InspectionResponseDto> startInspection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody InspectionCreateRequestDto requestDto) {
        Long inspectorId = userDetails.getAdminId();
        InspectionResponseDto response = inspectionService.startInspection(requestDto, inspectorId);
        return CustomApiResponse.success(response);
    }

    // ✅ 검수 합격
    @Override
    @PostMapping("/{inspectionId}/pass")
    @PreAuthorize("hasAnyRole('MASTER', 'INSPECTOR')")
    public CustomApiResponse<InspectionResponseDto> passInspection(
            @PathVariable UUID inspectionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody InspectionResultRequestDto requestDto) {
        Long inspectorId = userDetails.getAdminId();
        InspectionResponseDto response = inspectionService.passInspection(inspectionId, requestDto, inspectorId);
        return CustomApiResponse.success(response);
    }

    // ✅ 검수 불합격
    @Override
    @PostMapping("/{inspectionId}/fail")
    @PreAuthorize("hasAnyRole('MASTER', 'INSPECTOR')")
    public CustomApiResponse<InspectionResponseDto> failInspection(
            @PathVariable UUID inspectionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody InspectionResultRequestDto requestDto) {
        Long inspectorId = userDetails.getAdminId();
        InspectionResponseDto response = inspectionService.failInspection(inspectionId, requestDto, inspectorId);
        return CustomApiResponse.success(response);
    }

    // ✅ 주문별 검수 조회
    @Override
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('MASTER', 'INSPECTOR')")
    public CustomApiResponse<InspectionResponseDto> getInspection(
            @PathVariable UUID orderId) {
        InspectionResponseDto response = inspectionService.getInspectionByOrderId(orderId);
        return CustomApiResponse.success(response);
    }
}
