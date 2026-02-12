package com.example.unbox_payment.payment.domain.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 아웃박스 패턴을 위한 이벤트 저장 테이블
 * Payment 트랜잭션과 함께 저장되어 메시지 발행의 원자성을 보장
 */
@Entity
@Table(name = "payment_outbox", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentOutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "outbox_event_id", updatable = false, nullable = false)
    private UUID id;

    // 집합(Aggregate) 타입 (예: "PAYMENT")
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    // 집합 ID (결제 ID 등)
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    // 이벤트 타입 (예: "PaymentCompleted", "PaymentFailed")
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    // 이벤트 페이로드 (JSON 형식)
    @Setter
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    // 발행 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentOutboxEventStatus status;

    // 재시도 횟수
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    // 발행 완료 시각
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // 에러 메시지 (실패 시)
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // 발행 중 처리
    public void markAsProcessing() {
        this.status = PaymentOutboxEventStatus.PROCESSING;
        this.errorMessage = null;
    }

    // 발행 성공 처리
    public void markAsPublished() {
        this.status = PaymentOutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    // 발행 실패 처리 (재시도 가능)
    public void markAsRetry(String errorMessage) {
        this.status = PaymentOutboxEventStatus.PENDING; // 재시도를 위해 PENDING 상태로 복원
        this.retryCount++;
        this.errorMessage = truncateErrorMessage(errorMessage);
    }

    // 발행 실패 처리 (재시도 가능) - 에러 메시지 없는 버전
    public void markAsRetryPending() {
        this.status = PaymentOutboxEventStatus.PENDING;
        this.retryCount++;
    }

    // 발행 실패 처리 (최종 실패)
    public void markAsFailed(String errorMessage) {
        this.status = PaymentOutboxEventStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    // 발행 실패 처리 (최종 실패) - 에러 메시지 없는 버전
    public void markAsFailed() {
        this.status = PaymentOutboxEventStatus.FAILED;
    }

    // 에러 메시지 길이 제한 (DB 성능 보호)
    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        final int MAX_LENGTH = 1000;
        return errorMessage.length() > MAX_LENGTH
                ? errorMessage.substring(0, MAX_LENGTH) + "... (truncated)"
                : errorMessage;
    }
}
