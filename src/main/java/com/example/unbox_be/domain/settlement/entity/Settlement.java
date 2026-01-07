package com.example.unbox_be.domain.settlement.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_settlements")
@Getter
@Builder
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자 (빌더가 내부적으로 사용)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@org.hibernate.annotations.SQLDelete(sql = "UPDATE p_settlements SET deleted_at = NOW() WHERE settlements_id = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
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
    private Integer totalAmount;

    @Column(name = "settlement_amount", nullable = false)
    private Integer settlementAmount;

    @Column(name = "fees_amount", nullable = false)
    private Integer feesAmount;

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
