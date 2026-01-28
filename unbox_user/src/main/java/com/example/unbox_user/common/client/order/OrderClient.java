package com.example.unbox_user.common.client.order;

import com.example.unbox_user.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_user.common.client.order.dto.OrderForReviewInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

public interface OrderClient {

    // 주문 조회 (리뷰용)
    @GetMapping("/internal/order/{id}/for-review")
    OrderForReviewInfoResponse getOrderForReview (@PathVariable UUID id);

    // 주문 조회 (결제용)
    @GetMapping("/internal/order/{id}/for-payment")
    OrderForPaymentInfoResponse getOrderForPayment (@PathVariable UUID id);

    // 주문 상태 변경 (결제 완료용: PAYMENT_PENDING → PENDING_SHIPMENT)
    // 참고: 현재는 Kafka 이벤트 방식(OrderEventListener)으로 호출됨
    @PostMapping("/internal/order/{id}/pending-shipment")
    void pendingShipmentOrder (@PathVariable UUID id, 
                               @RequestParam UUID paymentId,
                               @RequestParam String updatedBy);
}
