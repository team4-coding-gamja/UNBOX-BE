package com.example.unbox_user.order.order.repository;

import com.example.unbox_user.order.order.dto.OrderSearchCondition;
import com.example.unbox_user.order.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderRepositoryCustom {
    Page<Order> findAdminOrders(OrderSearchCondition condition, Pageable pageable);
}
