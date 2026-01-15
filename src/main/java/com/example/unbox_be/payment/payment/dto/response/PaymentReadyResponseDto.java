package com.example.unbox_be.payment.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentReadyResponseDto(
        UUID paymentId,
        UUID orderId,
        BigDecimal price
) {}