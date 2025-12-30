package com.example.unbox_be.domain.order.repository;

import com.example.unbox_be.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}