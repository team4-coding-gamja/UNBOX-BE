package com.example.unbox_payment.common.client.order;

import com.example.unbox_payment.common.client.order.dto.OrderForPaymentInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "unbox-order", contextId = "orderClient", url = "${order-service.url}", path = "/order")
public interface OrderClient {

        // 주문 조회 (결제용)
        @GetMapping("/internal/orders/{id}/for-payment")
        OrderForPaymentInfoResponse getOrderForPayment(@PathVariable("id") UUID id,
                        @RequestHeader(value = "X-Fault-Target", required = false) String faultTarget,
                        @RequestHeader(value = "X-Fault-Delay-MS", required = false) Long faultDelay);

        // 주문 상태 변경 (결제 완료용: PAYMENT_PENDING → PENDING_SHIPMENT)
        // 참고: 현재는 Kafka 이벤트 방식(OrderEventListener)으로 호출됨
        @PostMapping("/internal/orders/{id}/pending-shipment")
        void pendingShipmentOrder(@PathVariable("id") UUID id,
                        @RequestParam("paymentId") UUID paymentId,
                        @RequestParam("updatedBy") String updatedBy,
                        @RequestHeader(value = "X-Test-Mode", required = false) String testMode,
                        @RequestHeader(value = "X-Fault-Target", required = false) String faultTarget,
                        @RequestHeader(value = "X-Fault-Delay-MS", required = false) Long faultDelay);
}
