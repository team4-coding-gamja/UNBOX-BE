package com.example.unbox_order.order.presentation.dto.response.inspection;

import com.example.unbox_order.order.domain.entity.InspectionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InspectionResponseDto {

    private UUID inspectionId;
    private UUID orderId;
    private Long inspectorId;
    private InspectionStatus status;
    private String reason;
    private LocalDateTime completedAt;
}
