package com.example.unbox_be.domain.payment.dto.response;

import java.util.UUID;

public record PaymentReadyResponseDto(
        UUID paymentId,
        String paymentKey
) {}