package com.example.unbox_be.payment.payment.service;

import com.example.unbox_be.order.entity.Order;
import com.example.unbox_be.order.entity.OrderStatus;
import com.example.unbox_be.order.repository.OrderRepository;
import com.example.unbox_be.payment.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.payment.payment.entity.Payment;
import com.example.unbox_be.payment.payment.entity.PaymentMethod;
import com.example.unbox_be.payment.payment.entity.PaymentStatus;
import com.example.unbox_be.payment.payment.repository.PaymentRepository;
import com.example.unbox_be.payment.settlement.service.SettlementService;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final TossApiService tossApiService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final SettlementService settlementService;

    //결제 생성하기
    @Transactional
    public PaymentReadyResponseDto createPayment(Long currentUserId, UUID orderId, PaymentMethod method) {
        // 주문 검사
        Order order = getAndValidateOrder(orderId, currentUserId);

        if(method == null){
            throw new CustomException(ErrorCode.PAYMENT_METHOD_INVALID);
        }

        Optional<Payment> existingPayment = paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId);

        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();

            // 이미 성공한 경우가 있을 때
            if (payment.getStatus() == PaymentStatus.DONE) {
                throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
            }
            if (payment.getStatus() == PaymentStatus.READY) {
                return new PaymentReadyResponseDto(
                        payment.getId(),
                        order.getId(),
                        payment.getAmount() // order.getPrice() 대신 저장된 금액 사용 권장
                );
            }
        }

        // 결제 생성
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(order.getPrice())
                .method(method)
                .status(PaymentStatus.READY)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        // ID와 가짜 키를 함께 반환
        return new PaymentReadyResponseDto(
                savedPayment.getId(),
                order.getId(),
                order.getPrice()
        );
    }

    public TossConfirmResponse confirmPayment(Long userId, UUID paymentId, String paymentKeyFromFront) {
        // PG사 승인 API 호출
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getStatus() == PaymentStatus.DONE) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }
        Order order = getAndValidateOrder(payment.getOrderId(), userId);

        BigDecimal amount = payment.getAmount();
        String finalPaymentKey = (paymentKeyFromFront == null || paymentKeyFromFront.isBlank())
                ? "mock_key_" + UUID.randomUUID().toString().substring(0, 8)
                : paymentKeyFromFront;
        TossConfirmResponse response = tossApiService.confirm(finalPaymentKey, amount);

        if (response.isSuccess()) {
            try {
                paymentTransactionService.processSuccessfulPayment(paymentId, response);
                settlementService.createSettlement(paymentId, order.getId());
                return response;
            } catch (Exception e) {
                log.error("결제 승인 후 서버 내부 처리 중 에러 발생 - 자동 취소 시도: {}", finalPaymentKey);
                tossApiService.cancel(finalPaymentKey, "서버 내부 오류로 인한 자동 취소");
                throw e;
            }
        } else {
            // 실패 시 처리 (로그 저장 등)
            paymentTransactionService.processFailedPayment(paymentId, response);
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    // 주문 정보 및 구매자 정보 검사
    private Order getAndValidateOrder(UUID orderId, Long userId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 1. 구매자 존재 여부 및 본인 확인
        if (order.getBuyer() == null || !order.getBuyer().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_SELF_ORDER_PAYMENT);
        }

        // 2. 주문 금액 검증
        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("주문 금액 정보가 올바르지 않습니다. OrderID: {}", order.getId());
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }

        // 3. 주문 상태 검증 (결제가 가능한 상태인지)
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        return order;
    }

}