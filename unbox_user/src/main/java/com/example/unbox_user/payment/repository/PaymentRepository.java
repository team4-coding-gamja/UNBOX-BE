package com.example.unbox_user.payment.repository;

import com.example.unbox_user.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // ✅ 결제 ID로 조회 (삭제된 데이터 제외)
    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);

    // ✅ 주문 ID로 결제 내역 조회 (삭제된 데이터 제외)
    @Query("select p from Payment p where p.orderId = :orderId and p.deletedAt is null")
    Optional<Payment> findByOrderIdAndDeletedAtIsNull(@Param("orderId") UUID orderId);

    // ✅ 주문 ID로 결제 존재 여부 확인 (중복 결제 방지용)
    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    // ✅ PG 결제 키로 조회 (취소/조회 시 사용)
    @Query("select p from Payment p where p.pgPaymentKey = :receiptKey and p.deletedAt is null")
    Optional<Payment> findByPgPaymentKeyAndDeletedAtIsNull(@Param("receiptKey") String receiptKey);

    // ✅ 완료된 결제 내역 조회 (주문 ID 기준)
    @Query("select p from Payment p where p.orderId = :orderId and p.status = 'DONE' and p.deletedAt is null")
    Optional<Payment> findDonePaymentByOrderId(@Param("orderId") UUID orderId);

}