package com.example.unbox_user.payment.dto.request;

import com.example.unbox_user.payment.entity.PaymentMethod;

import java.util.UUID;

public record PaymentCreateRequestDto(
        UUID orderId,
        PaymentMethod method
) {}