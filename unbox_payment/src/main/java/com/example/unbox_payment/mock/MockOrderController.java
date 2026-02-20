package com.example.unbox_payment.mock;

import com.example.unbox_payment.common.client.order.dto.OrderForPaymentInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/mock/order/internal/orders")
public class MockOrderController {

    private static final Logger log = LoggerFactory.getLogger(MockOrderController.class);

    @GetMapping("/{id}/for-payment")
    public OrderForPaymentInfoResponse getOrderForPayment(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Fault-Target", required = false) String faultTarget,
            // 결제 전 단계의 Order 조회에서는 일부러 딜레이를 주지 않을 수도 있지만 범용적으로 넣음
            @RequestHeader(value = "X-Fault-Delay-MS", required = false, defaultValue = "0") long delayMs)
            throws InterruptedException {

        log.info("[MockOrder] Received getOrderForPayment. orderId: {}, FaultTarget: {}, DelayMS: {}", id, faultTarget,
                delayMs);

        // 장애 주입: 타겟이 order이고 지연시간이 설정된 경우 모의 지연 발생
        if ("order".equalsIgnoreCase(faultTarget) && delayMs > 0) {
            log.warn("[MockOrder] Injecting {} ms delay for getOrderForPayment: {}", delayMs, id);
            Thread.sleep(delayMs);
        }

        return OrderForPaymentInfoResponse.builder()
                .orderId(id)
                .status("PAYMENT_PENDING") // 결제 가능한 상태
                .price(new BigDecimal("10000"))
                .buyerId(1L)
                .build();
    }

    // 동기식 상태 업데이트 호출 엔드포인트 Mock
    @PostMapping("/{id}/pending-shipment")
    public void pendingShipmentOrder(
            @PathVariable("id") UUID id,
            @RequestParam("paymentId") UUID paymentId,
            @RequestParam("updatedBy") String updatedBy,
            @RequestHeader(value = "X-Fault-Target", required = false) String faultTarget,
            @RequestHeader(value = "X-Fault-Delay-MS", required = false, defaultValue = "0") long delayMs)
            throws InterruptedException {

        log.info("[MockOrder] Received pendingShipmentOrder. orderId: {}, paymentId: {}, FaultTarget: {}, DelayMS: {}",
                id, paymentId, faultTarget, delayMs);

        // 장애 주입: 타겟이 order이고 지연시간이 설정된 경우 모의 지연 발생 (바로 이 부분에서 타임아웃/지연 전파 관찰 목적)
        if ("order".equalsIgnoreCase(faultTarget) && delayMs > 0) {
            log.warn("[MockOrder] Injecting {} ms delay for pendingShipmentOrder: {}", delayMs, id);
            Thread.sleep(delayMs);
        }
    }
}
