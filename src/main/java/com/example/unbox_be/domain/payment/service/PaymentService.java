package com.example.unbox_be.domain.payment.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.domain.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.domain.payment.entity.Payment;
import com.example.unbox_be.domain.payment.entity.PaymentMethod;
import com.example.unbox_be.domain.payment.entity.PaymentStatus;
import com.example.unbox_be.domain.payment.repository.PaymentRepository;
import com.example.unbox_be.domain.settlement.service.SettlementService;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
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
                String mockPaymentKey = "mock_key_" + UUID.randomUUID().toString().substring(0, 8);
                return new PaymentReadyResponseDto(payment.getId(), mockPaymentKey);
            }
        }

        // 결제 생성
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(order.getPrice().intValue())
                .method(method)
                .status(PaymentStatus.READY)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        // [테스트용] 가짜 키 생성
        String mockPaymentKey = "mock_key_" + UUID.randomUUID().toString().substring(0, 8);

        // ID와 가짜 키를 함께 반환
        return new PaymentReadyResponseDto(savedPayment.getId(), mockPaymentKey);
    }

    public void confirmPayment(Long userId, UUID paymentId, String paymentKeyFromFront) {
        // PG사 승인 API 호출
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getStatus() == PaymentStatus.DONE) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }
        Order order = getAndValidateOrder(payment.getOrderId(), userId);

        Integer amount = payment.getAmount();
        TossConfirmResponse response = tossApiService.confirm(paymentKeyFromFront, amount);

        if (response.isSuccess()) {
            String confirmedReceiptKey = response.getPaymentKey();
            try {

                // [수정 포인트 2] 이후 트랜잭션 기록 저장 및 추가 비즈니스 로직 처리
                paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKeyFromFront);
                settlementService.createSettlement(paymentId, order.getId());
            } catch (Exception e) {
                log.error("결제 승인 후 서버 내부 처리 중 에러 발생 - 자동 취소 시도: {}", confirmedReceiptKey);
                tossApiService.cancel(confirmedReceiptKey, "서버 내부 오류로 인한 자동 취소");
                throw e;
            }
        } else {
            // 실패 시 처리 (로그 저장 등)
            paymentTransactionService.processFailedPayment(paymentId, response);
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