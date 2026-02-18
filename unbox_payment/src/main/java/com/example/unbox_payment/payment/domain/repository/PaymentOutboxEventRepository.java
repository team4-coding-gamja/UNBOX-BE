package com.example.unbox_payment.payment.domain.repository;

import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEvent;
import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 아웃박스 이벤트 저장소
 */
@Repository
public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, UUID> {

    /**
     * 발행 대기 중인 이벤트 조회 (생성 시간 순)
     */
    List<PaymentOutboxEvent> findByStatusOrderByCreatedAtAsc(PaymentOutboxEventStatus status);

    /**
     * 특정 상태의 이벤트 개수 조회
     */
    long countByStatus(PaymentOutboxEventStatus status);

    /**
     * SKIP LOCKED를 사용한 비관적 락 조회
     * - 다중 인스턴스 환경에서 워커 간 병렬 처리를 위해 SKIP LOCKED 적용
     * - 다른 워커가 락을 보유한 Row는 건너뛰고(Skip) 다음 가용 Row를 즉시 조회
     * - Blocking 없이 워커들이 서로 다른 이벤트를 동시에 처리 가능
     * 
     * PostgreSQL의 FOR UPDATE SKIP LOCKED 구문을 사용:
     * - FOR UPDATE: 조회한 Row에 배타적 락(Exclusive Lock) 설정
     * - SKIP LOCKED: 이미 락이 걸린 Row는 건너뛰고 다음 Row 조회
     * 
     * @param status 조회할 이벤트 상태 (PENDING)
     * @param limit  조회할 최대 개수
     * @return 락을 획득한 이벤트 리스트
     */
    @Query(value = """
            SELECT * FROM payment_outbox
            WHERE status = CAST(:status AS VARCHAR)
            AND deleted_at IS NULL
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PaymentOutboxEvent> lockNextPending(@Param("status") String status, @Param("limit") int limit);

    /**
     * 특정 시간 이전에 생성된 특정 상태의 이벤트 조회 (복구용)
     * 
     * 워커 장애로 PROCESSING 상태로 멈춘 이벤트를 찾기 위해 사용
     * 예: 5분 이상 PROCESSING 상태인 이벤트 = 워커 장애로 멈춘 것으로 간주
     * 
     * @param status    조회할 이벤트 상태 (PROCESSING)
     * @param threshold 기준 시간 (이 시간 이전에 생성된 이벤트 조회)
     * @return 복구 대상 이벤트 리스트
     */
    @Query(value = """
            SELECT * FROM payment_outbox
            WHERE status = CAST(:status AS VARCHAR)
            AND created_at < :threshold
            AND deleted_at IS NULL
            ORDER BY created_at ASC
            """, nativeQuery = true)
    List<PaymentOutboxEvent> findStuckEvents(@Param("status") String status,
            @Param("threshold") java.time.LocalDateTime threshold);
}
