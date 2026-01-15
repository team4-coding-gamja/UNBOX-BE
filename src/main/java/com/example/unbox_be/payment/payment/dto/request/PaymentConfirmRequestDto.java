package com.example.unbox_be.payment.payment.dto.request;

import java.util.UUID;

public record PaymentConfirmRequestDto(
        UUID paymentId,
        String paymentKey
) {}