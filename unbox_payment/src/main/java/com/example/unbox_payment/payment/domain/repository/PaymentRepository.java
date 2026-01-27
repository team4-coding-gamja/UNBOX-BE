package com.example.unbox_payment.payment.domain.repository;

import com.example.unbox_payment.payment.domain.entity.Payment;
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
    // ✅ 주문 ID로 최신 결제 내역 조회 (삭제된 데이터 제외)
    Optional<Payment> findTopByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID orderId);

    // ✅ 주문 ID로 모든 결제 내역 조회 (삭제된 데이터 제외 - 정리용)
    java.util.List<Payment> findAllByOrderIdAndDeletedAtIsNull(UUID orderId);

    // ✅ 주문 ID로 결제 존재 여부 확인 (중복 결제 방지용)
    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    // ✅ PG 결제 키로 조회 (취소/조회 시 사용) - pgPaymentKey → paymentKey로 변경
    @Query("select p from Payment p where p.paymentKey = :paymentKey and p.deletedAt is null")
    Optional<Payment> findByPaymentKeyAndDeletedAtIsNull(@Param("paymentKey") String paymentKey);

    // ✅ 완료된 결제 내역 조회 (주문 ID 기준)
    @Query("select p from Payment p where p.orderId = :orderId and p.status = 'DONE' and p.deletedAt is null")
    Optional<Payment> findDonePaymentByOrderId(@Param("orderId") UUID orderId);

    // ✅ 구매자 ID로 결제 목록 조회
    java.util.List<Payment> findAllByBuyerIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long buyerId);
}