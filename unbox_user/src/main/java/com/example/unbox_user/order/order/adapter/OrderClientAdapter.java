package com.example.unbox_user.order.order.adapter;

import com.example.unbox_user.common.client.order.OrderClient;
import com.example.unbox_user.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_user.order.order.entity.Order;
import com.example.unbox_user.order.order.mapper.OrderClientMapper;
import com.example.unbox_user.order.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderClientAdapter implements OrderClient {

    private final OrderClientMapper orderClientMapper;
    private final OrderRepository orderRepository;

    // ✅ 주문 조회 (리뷰용)
    @Override
    public OrderForReviewInfoResponse getOrderForReview (UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        return orderClientMapper.toOrderForReviewInfoResponse(order);
    }
}
