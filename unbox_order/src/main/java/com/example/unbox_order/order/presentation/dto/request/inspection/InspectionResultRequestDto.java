package com.example.unbox_order.order.presentation.dto.request.inspection;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InspectionResultRequestDto {

    private String reason; // 필수 아님 (합격 시 비고, 불합격 시 사유)
}
