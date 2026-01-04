package com.example.unbox_be.domain.payment.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false) // 수정 불가능하게 설정
    private UUID orderId;

    @Column(name = "payment_amount", nullable = false)
    private Integer amount;

    @Column(name = "payment_method", nullable = false)
    private String method;

    @Column(name = "pg_payment_receipt_key")
    private String pgPaymentReceiptKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    public void completePayment(String pgPaymentKey) {
        this.pgPaymentReceiptKey = pgPaymentKey;
        this.status = PaymentStatus.DONE;
        this.capturedAt = LocalDateTime.now();
    }

    public void failPayment() {
        this.status = PaymentStatus.FAILED;
    }

    public void changeStatus(PaymentStatus status) {
        this.status = status;
    }
}
