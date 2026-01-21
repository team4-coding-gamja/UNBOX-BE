package com.example.unbox_user.payment.dto.response;

import com.example.unbox_user.payment.entity.PgTransaction;
import com.example.unbox_user.payment.entity.PgTransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PgTransactionResponse(
        UUID transactionId,
        String pgPaymentKey,
        String eventType,
        PgTransactionStatus eventStatus,
        LocalDateTime createdAt,
        String rawPayload // 필요 시 상세 데이터 포함
) {
    public static PgTransactionResponse from(PgTransaction entity) {
        return new PgTransactionResponse(
                entity.getId(),
                entity.getPgPaymentKey(),
                entity.getEventType(),
                entity.getEventStatus(),
                entity.getCreatedAt(),
                entity.getRawPayload()
        );
    }
}