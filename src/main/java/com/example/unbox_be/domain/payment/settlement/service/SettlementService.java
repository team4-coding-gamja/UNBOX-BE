package com.example.unbox_be.domain.payment.settlement.service;

import com.example.unbox_be.domain.order.order.entity.Order;
import com.example.unbox_be.domain.order.order.repository.OrderRepository;
import com.example.unbox_be.domain.payment.payment.entity.Payment;
import com.example.unbox_be.domain.payment.payment.repository.PaymentRepository;
import com.example.unbox_be.domain.payment.settlement.dto.response.SettlementResponseDto;
import com.example.unbox_be.domain.payment.settlement.entity.Settlement;
import com.example.unbox_be.domain.payment.settlement.entity.SettlementStatus;
import com.example.unbox_be.domain.payment.settlement.repository.SettlementRepository;
import com.example.unbox_be.domain.trade.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.trade.repository.SellingBidRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
    private static final double FEE_RATE = 0.03;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final SellingBidRepository sellingBidRepository;

    @Transactional
    public SettlementResponseDto createSettlement(UUID paymentId, UUID orderId){
        if (settlementRepository.existsByOrderId(orderId)) {
            throw new CustomException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException((ErrorCode.PAYMENT_NOT_FOUND)));

        SellingBid sellingBid = sellingBidRepository.findById(order.getSellingBidId())
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
        
        // seller 검증
        if (order.getSeller() == null) {
            throw new CustomException(ErrorCode.SETTLEMENT_SELLER_MISMATCH);
        }
        
        if (!payment.getOrderId().equals(orderId)) {
            throw new CustomException(ErrorCode.PAYMENT_SETTLEMENT_MISMATCH);
        }

        if (!order.getSeller().getId().equals(sellingBid.getSellerId())) {
            throw new CustomException(ErrorCode.SETTLEMENT_SELLER_MISMATCH);
        }

        BigDecimal totalAmount = payment.getAmount();
        BigDecimal fees = totalAmount.multiply(BigDecimal.valueOf(FEE_RATE))
                .setScale(0, java.math.RoundingMode.HALF_UP);

        BigDecimal settlementAmount = totalAmount.subtract(fees);

        Settlement settlement = Settlement.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .sellerId(order.getSeller().getId())
                .totalAmount(totalAmount)
                .feesAmount(fees)
                .settlementAmount(settlementAmount)
                .settlementStatus(SettlementStatus.WAITING) // 초기 상태
                .build();

        Settlement savedSettlement = settlementRepository.save(settlement);
        return SettlementResponseDto.from(savedSettlement);
    }

    @Transactional
    public SettlementResponseDto confirmSettlement(UUID orderId) {
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
        if (settlement.getSettlementStatus() != SettlementStatus.WAITING) {
            // 이미 DONE이면 ALREADY_DONE, 그 외(CANCELLED 등)면 INVALID_STATUS
            if (settlement.getSettlementStatus() == SettlementStatus.DONE) {
                throw new CustomException(ErrorCode.SETTLEMENT_ALREADY_DONE);
            }
            throw new CustomException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }
        settlement.updateStatus(SettlementStatus.DONE);
        return SettlementResponseDto.from(settlement);
    }
}
