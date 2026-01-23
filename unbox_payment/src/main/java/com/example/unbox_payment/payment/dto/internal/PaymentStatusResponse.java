package com.example.unbox_payment.payment.dto.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class PaymentStatusResponse {
    private UUID orderId;
    private String status;

    @Builder
    public PaymentStatusResponse(UUID orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }
}
