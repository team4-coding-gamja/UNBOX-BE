package com.example.unbox_be.domain.order.service;

import com.example.unbox_be.domain.order.dto.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.mapper.OrderMapper;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OrderMapper orderMapper;

    /**
     * 주문 생성 (즉시 구매)
     * - buyerEmail: 로그인한 사용자의 이메일 (CustomUserDetails에서 가져옴)
     */
    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDto requestDto, String buyerEmail) {
        // 1. 구매자 조회 (이메일로 조회)
        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 판매자 조회
        User seller = userRepository.findById(requestDto.getSellerId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 상품 옵션 조회
        ProductOption productOption = productOptionRepository.findById(requestDto.getProductOptionId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 4. Entity 생성
        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(productOption)
                .price(requestDto.getPrice())
                .receiverName(requestDto.getReceiverName())
                .receiverPhone(requestDto.getReceiverPhone())
                .receiverAddress(requestDto.getReceiverAddress())
                .receiverZipCode(requestDto.getReceiverZipCode())
                .build();

        // 5. 저장 및 반환
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponseDto(savedOrder);
    }
}