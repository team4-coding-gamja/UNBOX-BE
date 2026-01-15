package com.example.unbox_be.domain.payment.payment.repository;

import com.example.unbox_be.domain.payment.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // ✅ 1. 단순 PK 조회 (삭제된 데이터 제외)
    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);

    // ✅ 2. 주문 ID(orderId)로 결제 내역 조회 (삭제된 데이터 제외)
    // 엔티티 필드명이 orderId(UUID)이므로 바로 비교합니다.
    @Query("select p from Payment p where p.orderId = :orderId and p.deletedAt is null")
    Optional<Payment> findByOrderIdAndDeletedAtIsNull(@Param("orderId") UUID orderId);

    // ✅ 3. 주문 ID로 결제 존재 여부 확인 (중복 결제 방지용)
    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    // ✅ 4. PG 영수증 키(pgPaymentReceiptKey)로 조회
    // 토스페이먼츠의 paymentKey 등이 이 필드에 저장되므로 취소/조회 시 사용됩니다.
    @Query("select p from Payment p where p.pgPaymentKey = :receiptKey and p.deletedAt is null")
    Optional<Payment> findByPgPaymentKeyAndDeletedAtIsNull(@Param("receiptKey") String receiptKey);

    // ✅ 5. 특정 상태의 결제 내역 확인 (필요 시)
    @Query("select p from Payment p where p.orderId = :orderId and p.status = 'DONE' and p.deletedAt is null")
    Optional<Payment> findDonePaymentByOrderId(@Param("orderId") UUID orderId);

}