package com.example.unbox_be.domain.payment.payment.dto.request;

import com.example.unbox_be.domain.payment.payment.entity.PaymentMethod;

import java.util.UUID;

public record PaymentCreateRequestDto(
        UUID orderId,
        PaymentMethod method
) {}