package com.example.unbox_order.settlement.domain.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_settlements")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Settlement extends BaseEntity {

    // ======================= 기본 ID =======================
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "settlement_id")
    private UUID id;

    // ======================= 토스 필수 필드 =======================
    @Column(name = "m_id", length = 14)
    private String mId; // 토스 상점 ID

    @Column(name = "payment_key", nullable = false, length = 200)
    private String paymentKey; // 토스 결제 키

    @Column(name = "transaction_key", length = 64)
    private String transactionKey; // 토스 거래 키

    // ======================= 연관 ID =======================
    @Column(name = "order_id", nullable = false)
    private UUID orderId; // 주문 ID

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId; // 결제 ID

    @Column(name = "seller_id", nullable = false)
    private Long sellerId; // 판매자 ID (인덱스용)

    // ======================= 결제 정보 =======================
    @Column(name = "method", length = 20)
    private String method; // 결제 수단 (카드, 가상계좌 등)

    @Column(name = "approved_at")
    private LocalDateTime approvedAt; // 결제 승인 시각

    // ======================= 금액 정보 =======================
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // 결제 총 금액

    @Column(name = "fees_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal feesAmount; // 수수료

    @Column(name = "payout_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal payOutAmount; // 지급 금액 (totalAmount - feesAmount)

    // ======================= 정산 일정 =======================
    @Column(name = "sold_date")
    private LocalDate soldDate; // 정산 매출일 (D+1)

    @Column(name = "paidout_date")
    private LocalDate paidOutDate; // 정산 지급 예정일 (D+7)

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 실제 지급 완료 시각

    // ======================= 정산 상태 =======================
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementStatus status;

    // ======================= 판매자 정보 =======================
    @Column(name = "seller_name", length = 100)
    private String sellerName; // 판매자 닉네임 (화면 표시용)

    // ======================= 판매자 계좌 정보 =======================
    @Column(name = "seller_bank_name", length = 50)
    private String sellerBankName; // 은행명

    @Column(name = "seller_account_number", length = 50)
    private String sellerAccountNumber; // 계좌번호

    @Column(name = "seller_account_holder", length = 50)
    private String sellerAccountHolder; // 예금주명

    // ======================= 비즈니스 로직 메서드 =======================

    /**
     * 정산 지급 완료 처리
     */
    public void markAsPaidOut() {
        this.status = SettlementStatus.PAID_OUT;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 정산 취소 처리
     */
    public void cancel() {
        this.status = SettlementStatus.CANCELLED;
    }

    /**
     * 정산 상태 변경 (기존 호환성 유지)
     */
    public void updateStatus(SettlementStatus status) {
        this.status = status;
        if (status == SettlementStatus.PAID_OUT) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
