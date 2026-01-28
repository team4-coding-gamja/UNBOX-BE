package com.example.unbox_payment.payment.presentation.dto.request;

import com.example.unbox_payment.payment.domain.entity.PaymentMethod;

import java.util.UUID;

public record PaymentCreateRequestDto(
        UUID orderId,
        PaymentMethod method
) {}