package com.example.unbox_be.domain.payment.dto.request;

import java.util.UUID;

public record PaymentCreateRequestDto(
        UUID orderId,
        String method
) {}