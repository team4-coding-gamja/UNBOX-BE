package com.example.unbox_be.domain.settlement.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.payment.entity.Payment;
import com.example.unbox_be.domain.payment.repository.PaymentRepository;
import com.example.unbox_be.domain.settlement.entity.Settlement;
import com.example.unbox_be.domain.settlement.entity.SettlementStatus;
import com.example.unbox_be.domain.settlement.repository.SettlementRepository;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final SellingBidRepository sellingBidRepository;

    public void createSettlement(UUID paymentId, UUID orderId){
        if (settlementRepository.existsByOrderId(orderId)) {
            throw new CustomException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException((ErrorCode.PAYMENT_NOT_FOUND)));

        SellingBid sellingBid = sellingBidRepository.findById(order.getSellingBidId())
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        if (!payment.getOrderId().equals(orderId)) {
            throw new CustomException(ErrorCode.PAYMENT_SETTLEMENT_MISMATCH);
        }

        if (!order.getSeller().getId().equals(sellingBid.getUserId())) {
            throw new CustomException(ErrorCode.SETTLEMENT_SELLER_MISMATCH);
        }

        Integer totalAmount = payment.getAmount();
        Integer fees = (int)(totalAmount * 0.03);
        Integer settlementAmount = totalAmount - fees;

        Settlement settlement = Settlement.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .sellerId(order.getSeller().getId())
                .totalAmount(totalAmount)
                .feesAmount(fees)
                .settlementAmount(settlementAmount)
                .settlementStatus(SettlementStatus.WAITING) // 초기 상태
                .build();

        settlementRepository.save(settlement);
    }
    public void confirmSettlement(UUID orderId) {
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
        if (settlement.getSettlementStatus() == SettlementStatus.DONE) {
            throw new CustomException(ErrorCode.SETTLEMENT_ALREADY_DONE);
        }
        settlement.updateStatus(SettlementStatus.DONE);
        log.info("정산 완료 처리됨: OrderId={}, SettlementId={}", orderId, settlement.getId());
    }
}
