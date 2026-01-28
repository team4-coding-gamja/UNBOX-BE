package com.example.unbox_order.settlement.application.service;

import com.example.unbox_order.common.client.payment.PaymentClient;
import com.example.unbox_order.common.client.payment.dto.PaymentForSettlementResponse;
import com.example.unbox_order.common.client.user.UserClient;
import com.example.unbox_order.common.client.settlement.dto.SettlementCreateResponse;
import com.example.unbox_order.common.client.settlement.dto.SettlementForPaymentResponse;
import com.example.unbox_order.order.domain.entity.Order;
import com.example.unbox_order.order.domain.repository.OrderRepository;
import com.example.unbox_order.settlement.presentation.dto.response.SettlementResponseDto;
import com.example.unbox_order.settlement.domain.entity.Settlement;
import com.example.unbox_order.settlement.domain.entity.SettlementStatus;
import com.example.unbox_order.settlement.presentation.mapper.SettlementClientMapper;
import com.example.unbox_order.settlement.presentation.mapper.SettlementMapper;
import com.example.unbox_order.settlement.domain.repository.SettlementRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
    private static final double FEE_RATE = 0.03;

    private final OrderRepository orderRepository;
    private final SettlementRepository settlementRepository;
    private final PaymentClient paymentClient;
    private final UserClient userClient;
    private final SettlementClientMapper settlementClientMapper;
    private final SettlementMapper settlementMapper;

    // ✅ 정산 생성
    @Transactional
    public SettlementResponseDto createSettlement(UUID paymentId, UUID orderId) {
        if (settlementRepository.existsByOrderId(orderId)) {
            throw new CustomException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        PaymentForSettlementResponse paymentInfo = paymentClient.getPaymentForSettlement(paymentId);

        // seller 검증
        if (order.getSellerId() == null) {
            throw new CustomException(ErrorCode.SETTLEMENT_SELLER_MISMATCH);
        }

        if (!paymentInfo.getOrderId().equals(orderId)) {
            throw new CustomException(ErrorCode.PAYMENT_SETTLEMENT_MISMATCH);
        }

        BigDecimal totalAmount = paymentInfo.getAmount();
        BigDecimal fees = totalAmount.multiply(BigDecimal.valueOf(FEE_RATE))
                .setScale(0, java.math.RoundingMode.HALF_UP);

        BigDecimal settlementAmount = totalAmount.subtract(fees);

        Settlement settlement = Settlement.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .sellerId(order.getSellerId())
                .totalAmount(totalAmount)
                .feesAmount(fees)
                .payOutAmount(settlementAmount)
                .status(SettlementStatus.PENDING)
                .build();

        Settlement savedSettlement = settlementRepository.save(settlement);
        return settlementMapper.toSettlementResponseDto(savedSettlement);
    }

    // 정산 확정
    @Transactional
    public SettlementResponseDto confirmSettlement(UUID orderId) {
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
        if (settlement.getStatus() != SettlementStatus.PENDING) {
            // 이미 PAID_OUT이면 ALREADY_DONE, 그 외(CANCELLED 등)면 INVALID_STATUS
            if (settlement.getStatus() == SettlementStatus.PAID_OUT) {
                throw new CustomException(ErrorCode.SETTLEMENT_ALREADY_DONE);
            }
            throw new CustomException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        // ✅ 정산 계좌 존재 여부 확인
        if (!userClient.hasDefaultAccount(settlement.getSellerId())) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCOUNT_NOT_FOUND);
        }

        settlement.updateStatus(SettlementStatus.PAID_OUT); // PAID_OUT으로 변경
        return settlementMapper.toSettlementResponseDto(settlement);
    }

    // ✅ 정산 취소 (환불 시)
    @Transactional
    public void cancelSettlementByOrderId(UUID orderId) {
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElse(null);
        
        // 정산이 없으면 무시 (결제 직후 바로 취소된 경우 등)
        if (settlement == null) {
            log.warn("정산 없음, 취소 생략 - orderId: {}", orderId);
            return;
        }

        // 이미 취소된 경우 멱등성 보장
        if (settlement.getStatus() == SettlementStatus.CANCELLED) {
            log.warn("이미 취소된 정산 - settlementId: {}", settlement.getId());
            return;
        }

        // 이미 지급 완료된 경우 (관리자 개입 필요)
        if (settlement.getStatus() == SettlementStatus.PAID_OUT) {
            log.error("이미 지급 완료된 정산, 수동 처리 필요 - settlementId: {}", settlement.getId());
            return;
        }

        settlement.cancel();
        log.info("정산 취소 완료 - settlementId: {}, orderId: {}", settlement.getId(), orderId);
    }

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================

    // ✅ 정산 조회 (결제용)
    @Transactional(readOnly = true)
    public SettlementForPaymentResponse getSettlementForPayment(UUID settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));

        return settlementClientMapper.toSettlementForPaymentResponse(settlement);
    }

    // ✅ 정산 생성 (결제 완료 시)
    @Transactional
    public SettlementCreateResponse createSettlementForPayment(UUID paymentId) {

        // 1. 중복 생성 방지
        Optional<Settlement> existing = settlementRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.warn("정산이 이미 존재합니다. settlementId={}, paymentId={}",
                    existing.get().getId(), paymentId);
            return settlementClientMapper.toSettlementCreateResponse(existing.get());
        }

        // 2. Payment 정보 조회
        PaymentForSettlementResponse paymentInfo = paymentClient.getPaymentForSettlement(paymentId);

        // 3. 정산 금액 계산
        BigDecimal totalAmount = paymentInfo.getAmount();
        BigDecimal fees = totalAmount.multiply(BigDecimal.valueOf(FEE_RATE))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal settlementAmount = totalAmount.subtract(fees);

        // 4. 정산 생성
        Settlement settlement = Settlement.builder()
                .orderId(paymentInfo.getOrderId())
                .paymentId(paymentId)
                .sellerId(paymentInfo.getSellerId())
                .totalAmount(totalAmount)
                .feesAmount(fees)
                .payOutAmount(settlementAmount)
                .paymentKey(paymentInfo.getPaymentKey() != null ? paymentInfo.getPaymentKey() : "UNKNOWN")
                .status(SettlementStatus.PENDING)
                .build();

        Settlement savedSettlement = settlementRepository.save(settlement);
        log.info("정산 생성 완료: settlementId={}, paymentId={}, sellerId={}, amount={}",
                savedSettlement.getId(), paymentId, paymentInfo.getSellerId(), settlementAmount);

        return settlementClientMapper.toSettlementCreateResponse(savedSettlement);
    }
}
