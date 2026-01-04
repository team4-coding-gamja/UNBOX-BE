package com.example.unbox_be.domain.payment.repository;

import com.example.unbox_be.domain.payment.entity.PgTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PgTransactionRepository extends JpaRepository<PgTransaction, UUID> {

    // 특정 결제 건(paymentId)에 해당하는 모든 트랜잭션 로그를 생성일 역순으로 조회
    List<PgTransaction> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    // PG사에서 발급한 고유 키값으로 트랜잭션을 찾는 메서드 (취소/환불 시 필요)
    Optional<PgTransaction> findByPgPaymentKey(String pgPaymentKey);
}