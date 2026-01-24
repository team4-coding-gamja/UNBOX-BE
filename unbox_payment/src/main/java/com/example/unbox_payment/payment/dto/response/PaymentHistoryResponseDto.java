package com.example.unbox_payment.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentHistoryResponseDto(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String method,
        String status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt) {
}
