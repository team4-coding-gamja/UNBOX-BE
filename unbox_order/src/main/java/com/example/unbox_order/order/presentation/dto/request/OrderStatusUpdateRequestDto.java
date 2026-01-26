package com.example.unbox_order.order.dto.request;

import com.example.unbox_order.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequestDto {

    @NotNull(message = "변경할 주문 상태는 필수입니다.")
    private OrderStatus status;

    // 구매자에게 발송(SHIPPED_TO_BUYER)할 때만 필수
    private String trackingNumber;
}