package com.example.unbox_user.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentReadyResponseDto(
        UUID paymentId,
        UUID orderId,
        BigDecimal price
) {}