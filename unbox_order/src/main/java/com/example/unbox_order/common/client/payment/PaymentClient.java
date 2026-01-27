package com.example.unbox_order.common.client.payment;

import com.example.unbox_order.common.client.payment.dto.PaymentStatusResponse;
import com.example.unbox_order.common.client.payment.dto.PaymentForSettlementResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "unbox-payment", url = "${payment-service.url}", path = "/payment")
public interface PaymentClient {

    @GetMapping("/internal/payments/orders/{orderId}/status")
    PaymentStatusResponse getPaymentStatus(@PathVariable("orderId") UUID orderId);

    @GetMapping("/internal/payments/{paymentId}/for-settlement")
    PaymentForSettlementResponse getPaymentForSettlement(@PathVariable("paymentId") UUID paymentId);
}
