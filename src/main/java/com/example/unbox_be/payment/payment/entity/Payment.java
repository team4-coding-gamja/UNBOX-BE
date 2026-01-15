package com.example.unbox_be.payment.payment.entity;

import com.example.unbox_be.common.entity.BaseEntity;
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

    @Column(name = "order_id", nullable = false, updatable = false) // 수정 불가능하게 설정
    private UUID orderId;

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

    public void failPayment() {
        this.status = PaymentStatus.FAILED;
    }

    public void changeStatus(PaymentStatus nextStatus) {
        if (this.status == PaymentStatus.DONE && nextStatus != PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 완료된 결제는 상태를 변경할 수 없습니다.");
        }

        if (this.status == PaymentStatus.FAILED && nextStatus == PaymentStatus.DONE) {
            throw new IllegalStateException("실패한 결제를 성공 상태로 변경할 수 없습니다.");
        }

        // 3. 자기 자신과 같은 상태로 바꾸는 것은 무시하거나 허용
        if (this.status == nextStatus) return;

        this.status = nextStatus;
    }
}
