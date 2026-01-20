package com.example.unbox_be.payment.service;

import com.example.unbox_be.order.order.entity.Order;
import com.example.unbox_be.order.order.entity.OrderStatus;
import com.example.unbox_be.order.order.repository.OrderRepository;
import com.example.unbox_be.order.settlement.service.SettlementService;
import com.example.unbox_be.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.payment.entity.Payment;
import com.example.unbox_be.payment.entity.PaymentMethod;
import com.example.unbox_be.payment.entity.PaymentStatus;
import com.example.unbox_be.payment.mapper.PaymentMapper;
import com.example.unbox_be.payment.repository.PaymentRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    // ✅ 결제 가능한 주문 상태 정의
    private static final Set<OrderStatus> PAYABLE_STATUSES = Set.of(
            OrderStatus.PAYMENT_PENDING // 결제 대기 상태만 결제 가능
    );

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final SettlementService settlementService;
    private final TossApiService tossApiService;
    private final PaymentMapper paymentMapper;

    // ✅ 결제 준비 (초기 레코드 생성)
    @Override
    @Transactional
    public PaymentReadyResponseDto createPayment(Long userId, UUID orderId, PaymentMethod method) {
        // 결제 수단 유효성 검증
        if (method == null) {
            throw new CustomException(ErrorCode.PAYMENT_METHOD_INVALID);
        }

        // 주문 정보 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 구매자 존재 여부 및 본인 확인
        if (order.getBuyer() == null || !order.getBuyer().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_SELF_ORDER_PAYMENT);
        }

        // 주문 금액 검증
        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("주문 금액 정보가 올바르지 않습니다. OrderID: {}", order.getId());
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }

        // 주문 상태 검증 (결제 가능한 상태인지)
        if (!PAYABLE_STATUSES.contains(order.getStatus())) {
            log.error("결제 불가능한 주문 상태입니다. OrderID: {}, Status: {}", order.getId(), order.getStatus());
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 기존 결제 내역 확인
        Optional<Payment> existingPayment = paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId);

        // 기존 결제가 존재하는 경우 처리
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();

            // 이미 완료된 결제가 있는 경우 예외 발생
            if (payment.getStatus() == PaymentStatus.DONE) {
                throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
            }

            // 준비 상태인 경우 기존 정보 반환
            if (payment.getStatus() == PaymentStatus.READY) {
                return paymentMapper.toReadyResponseDto(payment, order);
            }
        }

        // 새로운 결제 엔티티 생성
        Payment payment = paymentMapper.toEntity(order, method);

        // 결제 정보 저장
        Payment savedPayment = paymentRepository.save(payment);

        // 응답 DTO 생성 및 반환
        return paymentMapper.toReadyResponseDto(savedPayment, order);
    }

    // ✅ 결제 승인 처리
    @Override
    @Transactional
    public TossConfirmResponse confirmPayment(Long userId, UUID paymentId, String paymentKeyFromFront) {
        // 결제 정보 조회
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 완료된 결제인지 확인
        if (payment.getStatus() == PaymentStatus.DONE) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        // 주문 정보 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(payment.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 구매자 존재 여부 및 본인 확인
        if (order.getBuyer() == null || !order.getBuyer().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_SELF_ORDER_PAYMENT);
        }

        // 주문 금액 검증
        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("주문 금액 정보가 올바르지 않습니다. OrderID: {}", order.getId());
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }

        // 주문 상태 검증 (결제 가능한 상태인지)
        if (!PAYABLE_STATUSES.contains(order.getStatus())) {
            log.error("결제 불가능한 주문 상태입니다. OrderID: {}, Status: {}", order.getId(), order.getStatus());
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 결제 금액 추출
        BigDecimal amount = payment.getAmount();

        // PG 결제 키 생성 (프론트에서 받은 키 또는 Mock 키)
        String finalPaymentKey = (paymentKeyFromFront == null || paymentKeyFromFront.isBlank())
                ? "mock_key_" + UUID.randomUUID().toString().substring(0, 8)
                : paymentKeyFromFront;

        // PG사 승인 API 호출
        TossConfirmResponse response = tossApiService.confirm(finalPaymentKey, amount);

        // 승인 결과에 따른 처리
        if (response.isSuccess()) {
            // 승인 성공 시 처리
            try {
                // 결제 트랜잭션 성공 처리
                paymentTransactionService.processSuccessfulPayment(paymentId, response);

                // 정산 정보 생성
                settlementService.createSettlement(paymentId, order.getId());

            } catch (Exception e) {
                // 예외 발생 시 로그 기록
                log.error("결제 승인 후 서버 내부 처리 중 에러 발생 - 자동 취소 시도: {}", finalPaymentKey);

                // PG사에 결제 취소 요청
                tossApiService.cancel(finalPaymentKey, "서버 내부 오류로 인한 자동 취소");

                // 예외 재발생
                throw e;
            }
            return response;
        } else {
            // 승인 실패 시 처리
            paymentTransactionService.processFailedPayment(paymentId, response);
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

}
