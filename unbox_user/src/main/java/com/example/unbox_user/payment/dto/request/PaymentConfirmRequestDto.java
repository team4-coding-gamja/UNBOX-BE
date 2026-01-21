package com.example.unbox_user.payment.dto.request;

import java.util.UUID;

public record PaymentConfirmRequestDto(
        UUID paymentId,
        String paymentKey
) {}