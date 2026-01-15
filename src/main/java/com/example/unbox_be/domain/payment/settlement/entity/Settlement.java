package com.example.unbox_be.domain.payment.settlement.entity;

import com.example.unbox_be.domain.common.BaseEntity;
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

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "payment_id",nullable = false)
    private UUID paymentId;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "settlement_amount", nullable = false)
    private BigDecimal settlementAmount;

    @Column(name = "fees_amount", nullable = false)
    private BigDecimal feesAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private SettlementStatus settlementStatus;

    @Column(name = "seller_bank_name")
    private String sellerBankName;

    @Column(name = "seller_account_number")
    private String sellerAccountNumber;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void updateStatus(SettlementStatus status){
        this.settlementStatus = status;
        if (status == SettlementStatus.DONE) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
