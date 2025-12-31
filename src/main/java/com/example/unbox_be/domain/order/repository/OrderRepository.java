package com.example.unbox_be.domain.order.repository;

import com.example.unbox_be.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    // 구매자 ID로 주문 내역 페이징 조회
    @EntityGraph(attributePaths = {"productOption", "productOption.product", "productOption.product.brand"})
    Page<Order> findAllByBuyerId(Long buyerId, Pageable pageable);
}