package com.example.unbox_be.domain.payment.dto.request;

import com.example.unbox_be.domain.payment.entity.PaymentMethod;

import java.util.UUID;

public record PaymentCreateRequestDto(
        UUID orderId,
        PaymentMethod method
) {}