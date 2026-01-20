package com.example.unbox_be.order.order.repository;

import com.example.unbox_be.order.order.dto.OrderSearchCondition;
import com.example.unbox_be.order.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderRepositoryCustom {
    Page<Order> findAdminOrders(OrderSearchCondition condition, Pageable pageable);
}
