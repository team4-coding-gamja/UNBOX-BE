package com.example.unbox_payment.payment.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "p_pg_transaction",
    indexes = {
        @Index(name = "idx_payment_key", columnList = "payment_key"),
        @Index(name = "idx_transaction_at", columnList = "transaction_at")
    }
)
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

    // ======================= 연관관계 =======================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // ======================= 토스 트랜잭션 필수 필드 =======================
    @Column(name = "transaction_key", unique = true, nullable = false, length = 64)
    private String transactionKey; // 거래 키 (승인/취소 구분)

    @Column(name = "payment_key", nullable = false, length = 200)
    private String paymentKey; // 결제 키

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId; // 주문 ID

    // ======================= 거래 정보 =======================
    @Column(name = "method", nullable = false)
    private String method; // 결제수단 (토스 응답값 그대로 저장)

    @Column(name = "customer_key", length = 300)
    private String customerKey; // 구매자 ID (토스 형식)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PgTransactionStatus status; // 거래 상태

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 거래 금액

    @Column(name = "transaction_at", nullable = false)
    private LocalDateTime transactionAt; // 거래 발생 시각

    // ======================= 추가 정보 =======================
    @Column(name = "currency", length = 3)
    private String currency; // 통화 (KRW)

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl; // 영수증 URL

    @Column(name = "use_escrow")
    private Boolean useEscrow; // 에스크로 사용 여부

    // ======================= 감사 로그 =======================
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse; // 토스 응답 원본 (JSON)

    // ======================= 비즈니스 로직 메서드 =======================

    /**
     * 거래 생성 (정적 팩토리 메서드)
     */
    public static PgTransaction createTransaction(
            Payment payment,
            String transactionKey,
            String paymentKey,
            String orderId,
            String method,
            String customerKey,
            PgTransactionStatus status,
            BigDecimal amount,
            LocalDateTime transactionAt,
            String rawResponse
    ) {
        return PgTransaction.builder()
                .payment(payment)
                .transactionKey(transactionKey)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .method(method)
                .customerKey(customerKey)
                .status(status)
                .amount(amount)
                .transactionAt(transactionAt)
                .currency("KRW")
                .useEscrow(false)
                .rawResponse(rawResponse)
                .build();
    }
}