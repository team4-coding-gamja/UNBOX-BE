package com.example.unbox_order.order.application.service;

import com.example.unbox_order.order.presentation.dto.request.inspection.InspectionCreateRequestDto;
import com.example.unbox_order.order.presentation.dto.request.inspection.InspectionResultRequestDto;
import com.example.unbox_order.order.presentation.dto.response.inspection.InspectionResponseDto;

import java.util.UUID;

public interface InspectionService {

    // 검수 시작
    InspectionResponseDto startInspection(InspectionCreateRequestDto requestDto, Long inspectorId);

    // 검수 합격
    InspectionResponseDto passInspection(UUID inspectionId, InspectionResultRequestDto requestDto, Long inspectorId);

    // 검수 불합격
    InspectionResponseDto failInspection(UUID inspectionId, InspectionResultRequestDto requestDto, Long inspectorId);

    // 검수 정보 조회
    InspectionResponseDto getInspectionByOrderId(UUID orderId);
}
