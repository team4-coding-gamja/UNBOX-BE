package com.example.unbox_payment.payment.presentation.dto.response;

import com.example.unbox_payment.payment.domain.entity.PgTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PgTransactionResponse(
        UUID transactionId,
        String transactionKey,
        String paymentKey,
        String orderId,
        String method,
        PgTransactionStatus status,
        BigDecimal amount,
        LocalDateTime transactionAt,
        String rawResponse
) {
}