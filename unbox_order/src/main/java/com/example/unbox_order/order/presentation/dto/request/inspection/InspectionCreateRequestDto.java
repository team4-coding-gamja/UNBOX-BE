package com.example.unbox_order.order.presentation.dto.request.inspection;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class InspectionCreateRequestDto {

    @NotNull(message = "주문 ID는 필수입니다.")
    private UUID orderId;
}
