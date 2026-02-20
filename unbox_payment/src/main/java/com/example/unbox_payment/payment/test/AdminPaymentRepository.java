package com.example.unbox_payment.payment.test;

import com.example.unbox_payment.payment.domain.entity.Payment;
import com.example.unbox_payment.payment.domain.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminPaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("""
            select p
            from Payment p
            where p.status = :status
            order by p.createdAt desc, p.id desc
            """)
    Page<Payment> searchAdminPayments(@Param("status") PaymentStatus status, Pageable pageable);

    @Query(value = """
            SELECT payment_id as id, order_id as orderId, buyer_id as buyerId, amount, status, created_at as createdAt
            FROM p_payment
            WHERE status = :#{#status.name()}
            ORDER BY created_at DESC, payment_id DESC
            OFFSET :offset LIMIT :limit
            """, nativeQuery = true)
    List<PaymentCoverageDto> findPaymentsForCoverageTest(
            @Param("status") PaymentStatus status,
            @Param("offset") long offset,
            @Param("limit") int limit);

    @Query(value = """
            EXPLAIN (ANALYZE, BUFFERS)
            SELECT payment_id, order_id, buyer_id, amount, status, created_at
            FROM p_payment
            WHERE status = :#{#status.name()}
            ORDER BY created_at DESC, payment_id DESC
            OFFSET :offset LIMIT :limit
            """, nativeQuery = true)
    List<String> explainPaymentsForCoverageTest(
            @Param("status") PaymentStatus status,
            @Param("offset") long offset,
            @Param("limit") int limit);
}
