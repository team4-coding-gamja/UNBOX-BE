package com.example.unbox_order.order.repository;

import com.example.unbox_order.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface AdminOrderRepository extends JpaRepository<Order, UUID>, AdminOrderRepositoryCustom {

    // [Admin] 주문 상세 조회
    Optional<Order> findWithDetailsById(@NonNull UUID id);
}
