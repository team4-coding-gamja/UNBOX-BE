package com.example.unbox_be.domain.payment.repository;

import com.example.unbox_be.domain.payment.entity.PgTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PgTransactionRepository extends JpaRepository<PgTransaction, UUID> {

    // ✅ 1. 특정 결제(paymentId)의 모든 이력 조회 (최신순)
    // 로그성 데이터라도 Soft Delete 정책을 따른다면 조건을 추가합니다.
    @Query("""
    select pt from PgTransaction pt
    where pt.payment.id = :paymentId
      and pt.deletedAt is null
    order by pt.createdAt desc
""")
    List<PgTransaction> findAllByPaymentId(@Param("paymentId") UUID paymentId);

    // ✅ 2. PG사 승인 번호(pgPaymentKey / transactionKey)로 조회
    // 결제 취소나 환불 시 해당 영수증 정보를 정확히 찾기 위해 사용합니다.
    Optional<PgTransaction> findByPgPaymentKeyAndDeletedAtIsNull(String pgPaymentKey);

    // ✅ 3. 특정 결제의 가장 최근 성공(DONE) 트랜잭션만
    @Query("""
    select pt from PgTransaction pt
    where pt.payment.id = :paymentId
      and pt.eventStatus = com.example.unbox_be.domain.payment.entity.PgTransactionStatus.DONE
      and pt.deletedAt is null
    order by pt.createdAt desc
    limit 1
""")
    Optional<PgTransaction> findLatestSuccessTransaction(@Param("paymentId") UUID paymentId);

    // ✅ 4. 결제 건당 트랜잭션 존재 여부 확인
    boolean existsByPaymentIdAndDeletedAtIsNull(UUID paymentId);
}