package com.example.unbox_payment.payment.test;

import com.example.unbox_payment.payment.domain.entity.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminPaymentSearchCondition {

    private PaymentStatus status;
    private Long buyerId;
    private Long sellerId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime readyAt;
}
