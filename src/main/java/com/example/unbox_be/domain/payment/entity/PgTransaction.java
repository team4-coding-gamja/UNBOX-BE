package com.example.unbox_be.domain.payment.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "p_pg_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class PgTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pg_transaction_id", updatable = false, nullable = false)
    private UUID id;

    // 결제 엔티티와의 연관관계 (FK: payment_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "pg_payment_key")
    private String pgPaymentKey; // PG사에서 발급한 거래 고유 키

    @Column(name = "pg_approve_no")
    private String pgApproveNo;

    @Column(name = "pg_provider")
    private String pgProvider;   // 결제 제공자 (예: TOSS, KAKAO)

    @Column(name = "event_type")
    private String eventType;    // PAYMENT, CANCEL 등

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status")
    private PgTransactionStatus eventStatus;  // DONE, READY, FAIL 등

    @Column(name = "event_amount")
    private BigDecimal eventAmount;

    // JSON 타입의 로그 저장
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "pg_seller_key")
    private String pgSellerKey;  // 가맹점 식별 키
}