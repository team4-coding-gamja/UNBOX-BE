package com.example.unbox_be.payment.dto.request;

import com.example.unbox_be.payment.entity.PaymentMethod;

import java.util.UUID;

public record PaymentCreateRequestDto(
        UUID orderId,
        PaymentMethod method
) {}