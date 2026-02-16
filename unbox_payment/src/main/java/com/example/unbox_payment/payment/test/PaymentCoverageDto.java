package com.example.unbox_payment.payment.test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface PaymentCoverageDto {
    UUID getId();

    UUID getOrderId();

    Long getBuyerId();

    BigDecimal getAmount();

    String getStatus();

    LocalDateTime getCreatedAt();
}
