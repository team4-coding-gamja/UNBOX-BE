package com.example.unbox_payment.payment.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID id;

    // ======================= 강한 ID 참조 =======================
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    // ======================= 약한 ID 참조 =======================
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // ======================= 결제 정보 =======================
    @Column(name = "payment_amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Column(name = "pg_payment_key")
    private String pgPaymentKey;

    @Column(name = "pg_approve_no")
    private String pgApproveNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Version
    private Long version;

    // ======================= 비즈니스 로직 메서드 =======================

    // ✅ 결제 완료 처리
    public void completePayment(String pgPaymentKey, String pgApproveNo) {
        if (this.status == PaymentStatus.DONE) {
            throw new IllegalStateException("이미 완료된 결제입니다.");
        }
        if (this.status == PaymentStatus.CANCELED) {
            throw new IllegalStateException("취소된 결제는 완료 처리할 수 없습니다.");
        }
        this.pgPaymentKey = pgPaymentKey;
        this.pgApproveNo = pgApproveNo;
        this.status = PaymentStatus.DONE;
        this.capturedAt = LocalDateTime.now();
    }

    // ✅ 결제 실패 처리
    public void failPayment() {
        this.status = PaymentStatus.FAILED;
    }

    // ✅ 결제 상태 변경
    public void changeStatus(PaymentStatus nextStatus) {
        // 완료된 결제는 취소만 가능
        if (this.status == PaymentStatus.DONE && nextStatus != PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 완료된 결제는 상태를 변경할 수 없습니다.");
        }

        // 실패한 결제를 성공으로 변경 불가
        if (this.status == PaymentStatus.FAILED && nextStatus == PaymentStatus.DONE) {
            throw new IllegalStateException("실패한 결제를 성공 상태로 변경할 수 없습니다.");
        }

        // 동일 상태로의 변경은 무시
        if (this.status == nextStatus)
            return;

        this.status = nextStatus;
    }
}
