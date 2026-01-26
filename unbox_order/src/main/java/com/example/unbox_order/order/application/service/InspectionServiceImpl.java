package com.example.unbox_order.order.application.service;

import com.example.unbox_order.order.domain.entity.Inspection;
import com.example.unbox_order.order.domain.repository.InspectionRepository;
import com.example.unbox_order.order.presentation.dto.request.inspection.InspectionCreateRequestDto;
import com.example.unbox_order.order.presentation.dto.request.inspection.InspectionResultRequestDto;
import com.example.unbox_order.order.presentation.dto.response.inspection.InspectionResponseDto;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRepository inspectionRepository;
    private final OrderService orderService; // 순환 참조 주의 (하지만 OrderService는 InspectionService를 참조하지 않으므로 안전)

    // ✅ 검수 시작 (레코드 생성 및 Order 상태 변경)
    @Override
    @Transactional
    public InspectionResponseDto startInspection(InspectionCreateRequestDto requestDto, Long inspectorId) {
        UUID orderId = requestDto.getOrderId();

        // 1. 이미 검수 진행 중인지 확인 (중복 생성 방지)
        if (inspectionRepository.existsByOrderIdAndDeletedAtIsNull(orderId)) {
             throw new CustomException(ErrorCode.INSPECTION_ALREADY_EXISTS);
        }
        
        // 2. 검수 엔티티 생성
        Inspection inspection = Inspection.builder()
                .orderId(orderId)
                .inspectorId(inspectorId)
                .build();
        
        inspection = inspectionRepository.save(inspection);

        // 3. 주문 상태 변경 (ARRIVED -> IN_INSPECTION)
        orderService.startInspection(orderId);

        return toResponseDto(inspection);
    }

    // ✅ 검수 합격 처리
    @Override
    @Transactional
    public InspectionResponseDto passInspection(UUID inspectionId, InspectionResultRequestDto requestDto, Long inspectorId) {
        // 1. 검수 조회
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 2. 검수관 본인 확인 (정책 강화: 본인이 아니면 접근 불가)
        if (!inspection.getInspectorId().equals(inspectorId)) {
            log.error("Inspector mismatch. Owner: {}, Requestor: {}", inspection.getInspectorId(), inspectorId);
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 검수 합격 처리 (Entity 상태 변경)
        String reason = (requestDto != null) ? requestDto.getReason() : null;
        inspection.pass(reason);

        // 4. 주문 상태 변경 (OrderService 연동)
        orderService.passedInspection(inspection.getOrderId());

        return toResponseDto(inspection);
    }

    // ✅ 검수 불합격 처리
    @Override
    @Transactional
    public InspectionResponseDto failInspection(UUID inspectionId, InspectionResultRequestDto requestDto, Long inspectorId) {
        // 1. 검수 조회
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 2. 검수 불합격 처리
        String reason = (requestDto != null) ? requestDto.getReason() : "불합격 사유 미입력";
        inspection.fail(reason);

        // 3. 주문 상태 변경 (OrderService 연동)
        orderService.failedInspection(inspection.getOrderId());

        return toResponseDto(inspection);
    }

    // ✅ 검수 조회
    @Override
    public InspectionResponseDto getInspectionByOrderId(UUID orderId) {
        Inspection inspection = inspectionRepository.findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSPECTION_NOT_FOUND)); 
        
        return toResponseDto(inspection);
    }

    private InspectionResponseDto toResponseDto(Inspection inspection) {
        return InspectionResponseDto.builder()
                .inspectionId(inspection.getId())
                .orderId(inspection.getOrderId())
                .inspectorId(inspection.getInspectorId())
                .status(inspection.getInspectStatus())
                .reason(inspection.getReason())
                .completedAt(inspection.getCompletedAt())
                .build();
    }
}
