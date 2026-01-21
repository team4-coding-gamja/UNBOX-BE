package com.example.unbox_payment.payment.dto.request;

import java.util.UUID;

public record PaymentConfirmRequestDto(
        UUID paymentId,
        String paymentKey
) {}