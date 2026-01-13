package com.example.unbox_be.domain.order.service;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.domain.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.mapper.OrderMapper;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.settlement.service.SettlementService;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.client.product.ProductClient;
import com.example.unbox_be.global.client.product.dto.ProductOptionForOrderInfoResponse;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final SellingBidRepository sellingBidRepository;
    private final SettlementService settlementService;
    private final ProductClient productClient;

    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public UUID createOrder(OrderCreateRequestDto requestDto, Long buyerId) {
        // 1. 구매자 조회 (Entity 필요)
        User buyer = getUserByIdOrThrow(buyerId);

        // 2. 락 없이 먼저 조회하여 Product 정보 확보 (외부 통신 지연 시 DB 락 점유 방지)
        SellingBid tempBid = sellingBidRepository.findByIdAndDeletedAtIsNull(requestDto.getSellingBidId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (tempBid.getProductOption() == null) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND); 
        }
        UUID productOptionId = tempBid.getProductOption().getId(); // NPE Guarded

        // 3. 상품 정보 조회 (ProductClient 사용) - 외부 통신
        ProductOptionForOrderInfoResponse productInfo;
        try {
             productInfo = productClient.getProductForOrder(productOptionId);
        } catch (Exception e) {
            // 외부 서비스 에러 등을 적절히 래핑
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND); // 혹은 EXTERNAL_API_ERROR
        }

        // 4. 판매 입찰 글(SellingBid) 재조회 (비관적 락 적용) - 이제 락 구간 진입
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(requestDto.getSellingBidId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (sellingBid.getStatus() == SellingStatus.MATCHED || sellingBid.getStatus() == SellingStatus.HOLD) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 5. 본인 판매글 구매 방지
        if (sellingBid.getUserId().equals(buyerId)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 6. 판매자 정보 조회 (Entity 필요)
        User seller = getUserByIdOrThrow(sellingBid.getUserId());

        // 7. 가격 타입 변환 (Integer -> BigDecimal)
        BigDecimal price = sellingBid.getPrice();
        
        // 8. Order 생성
        Order order = Order.builder()
                .sellingBidId(sellingBid.getId())
                .buyer(buyer)            // Entity 주입
                .seller(seller)          // Entity 주입
                .productOptionId(productInfo.getId())
                .productId(productInfo.getProductId())
                .productName(productInfo.getProductName())
                .modelNumber(productInfo.getModelNumber())
                .optionName(productInfo.getOptionName())
                .imageUrl(productInfo.getImageUrl())
                .brandName(productInfo.getBrandName())
                .price(price)
                .receiverName(requestDto.getReceiverName())
                .receiverPhone(requestDto.getReceiverPhone())
                .receiverAddress(requestDto.getReceiverAddress())
                .receiverZipCode(requestDto.getReceiverZipCode())
                .build();

        // 9. 판매 입찰 상태 변경 (판매 완료 처리)
        sellingBid.updateStatus(SellingStatus.MATCHED);

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
        User user = getUserByIdOrThrow(userId); // confirm 메서드가 User를 받을 수도 있으니 체크
        Order order = getOrderWithDetailsOrThrow(orderId);

        // Order.confirm 시그니처가 User인지 Long인지 확인 필요. 
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