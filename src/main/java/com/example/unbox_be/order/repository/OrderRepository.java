package com.example.unbox_be.order.repository;

import com.example.unbox_be.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // 구매자 ID로 주문 내역 페이징 조회(삭제 포함)
    @EntityGraph(attributePaths = {})
    Page<Order> findAllByBuyerId(Long buyerId, Pageable pageable);

    // 구매자 ID로 주문 내역 페이징 조회(삭제 제외)
    @EntityGraph(attributePaths = {})
    Page<Order> findAllByBuyerIdAndDeletedAtIsNull(Long buyerId, Pageable pageable);

    // 주문 상세 조회(삭제 포함)
    @EntityGraph(attributePaths = {"buyer", "seller"})
    Optional<Order> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"buyer", "seller"})
    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);
}