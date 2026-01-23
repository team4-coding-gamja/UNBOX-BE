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

    // ======================= 토스 페이먼츠 필수 필드 =======================
    @Column(name = "payment_key", unique = true, nullable = false, length = 200)
    private String paymentKey; // 토스 결제 키 (고유)

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId; // 주문 ID

    // ======================= 비즈니스 필수 필드 =======================
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId; // 구매자 ID

    @Column(name = "seller_id", nullable = false)
    private Long sellerId; // 판매자 ID

    // ======================= 결제 정보 =======================
    @Column(name = "method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod method; // 결제수단

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status; // 결제 상태

    @Column(name = "approved_at")
    private LocalDateTime approvedAt; // 결제 승인 시각

    @Version
    private Long version; // 낙관적 락

    // ======================= 비즈니스 로직 메서드 =======================

    /**
     * 결제 승인 완료 처리
     */
    public void completePayment(String paymentKey) {
        if (this.status == PaymentStatus.DONE) {
            throw new IllegalStateException("이미 완료된 결제입니다.");
        }
        if (this.status == PaymentStatus.CANCELED) {
            throw new IllegalStateException("취소된 결제는 완료 처리할 수 없습니다.");
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리
     */
    public void failPayment() {
        this.status = PaymentStatus.FAILED;
    }

    /**
     * 결제 취소 처리
     */
    public void cancelPayment() {
        if (this.status != PaymentStatus.DONE) {
            throw new IllegalStateException("완료된 결제만 취소할 수 있습니다.");
        }
        this.status = PaymentStatus.CANCELED;
    }

    /**
     * 결제 상태 변경
     */
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
        if (this.status == nextStatus) {
            return;
        }

        this.status = nextStatus;
    }
}