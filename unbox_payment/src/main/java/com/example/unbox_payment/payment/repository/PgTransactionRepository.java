package com.example.unbox_payment.payment.repository;

import com.example.unbox_payment.payment.entity.PgTransaction;
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
}