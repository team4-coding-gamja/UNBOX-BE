package com.example.unbox_be.domain.payment.repository;

import com.example.unbox_be.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // 주문 엔티티의 ID로 결제 내역을 조회하는 메서드
    // Order와의 연관관계가 설정되어 있어 findByOrder_Id 형식을 사용
    Optional<Payment> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);
}