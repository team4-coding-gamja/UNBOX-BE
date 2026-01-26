package com.example.unbox_order.order.domain.repository;

import com.example.unbox_order.order.presentation.dto.OrderSearchCondition;
import com.example.unbox_order.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderRepositoryCustom {
    Page<Order> findAdminOrders(OrderSearchCondition condition, Pageable pageable);
}
