package com.example.unbox_user.order.settlement.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
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

    @Id
    @Column(name = "settlements_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ======================= 강한 ID 참조 =======================
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    // ======================= 약한 ID 참조 =======================
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    // 판매자별 정산 조회 성능을 위한 비정규화 (Order.sellerId의 스냅샷)
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // ======================= 정산 금액 정보 =======================
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "settlement_amount", nullable = false)
    private BigDecimal settlementAmount;

    @Column(name = "fees_amount", nullable = false)
    private BigDecimal feesAmount;

    // ======================= 정산 상태 정보 =======================
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementStatus settlementStatus;

    // ======================= 판매자 계좌 정보 =======================
    @Column(name = "seller_bank_name")
    private String sellerBankName;

    @Column(name = "seller_account_number")
    private String sellerAccountNumber;

    // ======================= 정산 일정 정보 =======================
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ======================= 비즈니스 로직 메서드 =======================

    // ✅ 정산 상태 변경
    public void updateStatus(SettlementStatus status) {
        this.settlementStatus = status;
        if (status == SettlementStatus.CONFIRMED) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
