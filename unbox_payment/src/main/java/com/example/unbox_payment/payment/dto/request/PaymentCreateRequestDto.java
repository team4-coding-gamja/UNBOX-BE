package com.example.unbox_payment.payment.dto.request;

import com.example.unbox_payment.payment.entity.PaymentMethod;

import java.util.UUID;

public record PaymentCreateRequestDto(
        UUID orderId,
        PaymentMethod method
) {}