package com.example.unbox_common.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이벤트 봉투 패턴 (Envelope Pattern)
 * 
 * 모든 Kafka 이벤트는 이 표준 규격을 따라야 합니다.
 * - eventId: 이벤트 고유 식별자
 * - eventType: 이벤트 타입 (PaymentCompleted, OrderCreated 등)
 * - occurredAt: 이벤트 발생 시각
 * - aggregateId: 집합 루트 ID (파티션 키로 사용)
 * - data: 실제 비즈니스 데이터 (Payload)
 * 
 * 이 구조를 사용하면:
 * 1. 컨슈머가 eventType만 보고 이벤트를 분류 가능
 * 2. 서비스 간 결합도 감소 (클래스 타입 의존성 제거)
 * 3. 디버깅 및 모니터링 용이
 * 4. 이벤트 추적 및 이력 관리 가능
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope {

    /**
     * 이벤트 고유 식별자 (UUID)
     * Outbox 테이블의 ID와 동일하게 설정
     */
    private UUID eventId;

    /**
     * 이벤트 타입 문자열
     * 예: "PaymentCompleted", "PaymentFailed", "OrderCreated"
     */
    private String eventType;

    /**
     * 이벤트 발생 시각
     */
    private LocalDateTime occurredAt;

    /**
     * 집합 루트 ID (Aggregate ID)
     * Kafka 파티션 키로 사용하여 순서 보장
     */
    private UUID aggregateId;

    /**
     * 실제 비즈니스 데이터
     * Jackson의 JsonNode 또는 Object로 저장
     */
    private Object data;

    /**
     * 편의 메서드: 이벤트 타입 확인
     */
    public boolean isEventType(String expectedType) {
        return expectedType != null && expectedType.equals(this.eventType);
    }
}
