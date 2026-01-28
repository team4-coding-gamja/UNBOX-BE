package com.example.unbox_payment.payment.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentConfirmRequestDto(
                UUID paymentId,
                String paymentKey,
                BigDecimal amount) {
}