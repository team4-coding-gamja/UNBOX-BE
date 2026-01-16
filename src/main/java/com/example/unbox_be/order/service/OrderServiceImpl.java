package com.example.unbox_be.order.service;

import com.example.unbox_be.user.admin.entity.Admin;
import com.example.unbox_be.user.admin.entity.AdminRole;
import com.example.unbox_be.user.admin.repository.AdminRepository;
import com.example.unbox_be.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_be.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.order.dto.response.OrderResponseDto;
import com.example.unbox_be.order.entity.Order;
import com.example.unbox_be.order.entity.OrderStatus;
import com.example.unbox_be.order.mapper.OrderMapper;
import com.example.unbox_be.order.repository.OrderRepository;
import com.example.unbox_be.product.product.infrastructure.adapter.ProductClientAdapter;
import com.example.unbox_be.payment.settlement.service.SettlementService;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import com.example.unbox_be.trade.domain.entity.SellingStatus;
import com.example.unbox_be.trade.domain.repository.SellingBidRepository;
import com.example.unbox_be.user.user.entity.User;
import com.example.unbox_be.user.user.repository.UserRepository;
import com.example.unbox_be.common.client.product.dto.ProductOptionForOrderInfoResponse;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final SellingBidRepository sellingBidRepository;
    private final SettlementService settlementService;
    private final ProductClientAdapter productClientAdapter;

    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public UUID createOrder(OrderCreateRequestDto requestDto, Long buyerId) {
        User buyer = getUserByIdOrThrow(buyerId);

        // 1) sellingBid 먼저 조회 (락 없음) - 스냅샷이 bid에 있으면 여기서 해결 가능
        SellingBid bid = sellingBidRepository.findByIdAndDeletedAtIsNull(requestDto.getSellingBidId())
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));

        if (Objects.equals(bid.getSellerId(), buyerId)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (bid.getProductOptionId() == null) {
            throw new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND);
        }

        // 2) 외부 조회(스냅샷 채우기) - 락 없음
        ProductOptionForOrderInfoResponse productInfo = productClientAdapter.getProductForOrder(bid.getProductOptionId());

        // 3) DB에서 원샷으로 "LIVE -> MATCHED" 선점 시도 (동시성 핵심)
        int updated = sellingBidRepository.updateStatusIfMatch(
                bid.getId(), SellingStatus.LIVE, SellingStatus.MATCHED
        );
        if (updated == 0) {
            // 이미 누가 매칭했거나 상태가 바뀜
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 4) 판매자 조회 (필요하면)
        User seller = getUserByIdOrThrow(bid.getSellerId());

        // 5) Order 생성 (스냅샷 저장)
        Order order = Order.builder()
                .sellingBidId(bid.getId())
                .buyer(buyer)
                .seller(seller)
                .productOptionId(productInfo.getProductOptionId())
                .productId(productInfo.getProductId())
                .productName(productInfo.getProductName())
                .modelNumber(productInfo.getModelNumber())
                .productOptionName(productInfo.getProductOptionName())
                .productImageUrl(productInfo.getProductImageUrl())
                .brandName(productInfo.getBrandName())
                .price(bid.getPrice())
                .receiverName(requestDto.getReceiverName())
                .receiverPhone(requestDto.getReceiverPhone())
                .receiverAddress(requestDto.getReceiverAddress())
                .receiverZipCode(requestDto.getReceiverZipCode())
                .build();

        return orderRepository.save(order).getId();
    }

    /**
     * 내 구매 내역 조회 (페이징)
     */
    @Override
    public Page<OrderResponseDto> getMyOrders(Long buyerId, Pageable pageable) {
        if (!userRepository.existsById(buyerId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return orderRepository.findAllByBuyerIdAndDeletedAtIsNull(buyerId, pageable)
                .map(orderMapper::toResponseDto);
    }

    /**
     * 주문 상세 조회
     */
    @Override
    public OrderDetailResponseDto getOrderDetail(UUID orderId, Long userId) {
        Order order = getOrderWithDetailsOrThrow(orderId);
        validateOrderReadAccess(order, userId);
        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 주문 취소 (판매자/구매자 공용)
     */
    @Override
    @Transactional
    public OrderDetailResponseDto cancelOrder(UUID orderId, Long userId) {
        Order order = getOrderWithDetailsOrThrow(orderId);

        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isSeller = order.getSeller().getId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        order.cancel();
        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 운송장 번호 등록 (판매자용)
     */
    @Override
    @Transactional
    public OrderDetailResponseDto registerTracking(UUID orderId, String trackingNumber, Long sellerId) {
        Order order = getOrderWithDetailsOrThrow(orderId);

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        order.registerTracking(trackingNumber);
        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 관리자/검수자 주문 상태 변경
     */
    @Override
    @Transactional
    public OrderDetailResponseDto updateAdminStatus(UUID orderId, OrderStatus newStatus, String finalTrackingNumber, Long adminId) {
        Admin admin = adminRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        if (admin.getAdminRole() != AdminRole.ROLE_MASTER && admin.getAdminRole() != AdminRole.ROLE_INSPECTOR) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        Order order = getOrderWithDetailsOrThrow(orderId);
        order.updateAdminStatus(newStatus, finalTrackingNumber);
        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 구매 확정 (구매자 전용)
     */
    @Override
    @Transactional
    public OrderDetailResponseDto confirmOrder(UUID orderId, Long userId) {
        User user = getUserByIdOrThrow(userId);
        Order order = getOrderWithDetailsOrThrow(orderId);

        order.confirm(user); 
        
        settlementService.confirmSettlement(orderId);
        return orderMapper.toDetailResponseDto(order);
    }

    // --- Private Helper Methods ---

    private User getUserByIdOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Order getOrderWithDetailsOrThrow(UUID orderId) {
        return orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateOrderReadAccess(Order order, Long userId) {
        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isSeller = order.getSeller().getId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }
}