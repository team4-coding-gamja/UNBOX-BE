package com.example.unbox_order.order.service;

import com.example.unbox_order.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_order.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_order.common.client.trade.dto.SellingBidForOrderResponse;
import com.example.unbox_order.common.client.trade.TradeClient;
import com.example.unbox_order.common.client.user.UserClient;
import com.example.unbox_order.common.client.user.dto.UserInfoForOrderResponse;
import com.example.unbox_order.order.mapper.OrderClientMapper;
import com.example.unbox_order.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_order.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_order.order.dto.response.OrderResponseDto;
import com.example.unbox_order.order.entity.Order;
import com.example.unbox_order.order.entity.OrderStatus;
import com.example.unbox_order.order.mapper.OrderMapper;
import com.example.unbox_order.order.repository.OrderRepository;
import com.example.unbox_order.settlement.service.SettlementService;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
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
    private final UserClient userClient;
    private final TradeClient tradeClient;
    private final SettlementService settlementService;
    private final OrderMapper orderMapper;
    private final OrderClientMapper orderClientMapper;

    // ✅ 주문 생성
    @Override
    @Transactional
    public UUID createOrder(OrderCreateRequestDto requestDto, Long buyerId) {
        // 1) 구매자 조회 (스냅샷 저장을 위해)
        UserInfoForOrderResponse buyer = userClient.getUserInfoForOrder(buyerId);

        // 2) 판매 입찰 정보 조회
        SellingBidForOrderResponse sellingBidInfo = tradeClient
                .getSellingBidForOrder(requestDto.getSellingBidId());

        // 3) 자기 자신의 상품 구매 방지
        if (Objects.equals(sellingBidInfo.getSellerId(), buyerId)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 4) 상품 옵션 존재 여부 확인
        if (sellingBidInfo.getProductOptionId() == null) {
            throw new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND);
        }

        // 5) 판매 입찰 선점 (LIVE → RESERVED)
        tradeClient.reserveSellingBid(sellingBidInfo.getSellingBidId(), "ORDER_SERVICE");

        // 6) 주문 생성 (스냅샷 저장)
        Order order = Order.builder()
                .sellingBidId(sellingBidInfo.getSellingBidId())
                .buyerId(buyerId)
                .sellerId(sellingBidInfo.getSellerId())
                .buyerName(buyer.getNickname()) // 구매자 닉네임 스냅샷
                .productOptionId(sellingBidInfo.getProductOptionId())
                .productId(sellingBidInfo.getProductId())
                .productName(sellingBidInfo.getProductName())
                .modelNumber(sellingBidInfo.getModelNumber())
                .productOptionName(sellingBidInfo.getProductOptionName())
                .productImageUrl(sellingBidInfo.getProductImageUrl())
                .brandName(sellingBidInfo.getBrandName())
                .price(sellingBidInfo.getPrice())
                .receiverName(requestDto.getReceiverName())
                .receiverPhone(requestDto.getReceiverPhone())
                .receiverAddress(requestDto.getReceiverAddress())
                .receiverZipCode(requestDto.getReceiverZipCode())
                .build();

        return orderRepository.save(order).getId();
    }

    // ✅ 내 구매 내역 조회 (페이징)
    @Override
    public Page<OrderResponseDto> getMyOrders(Long buyerId, Pageable pageable) {
        // 1) 주문 목록 조회 및 DTO 변환
        return orderRepository.findAllByBuyerIdAndDeletedAtIsNull(buyerId, pageable)
                .map(orderMapper::toResponseDto);
    }

    // ✅ 주문 상세 조회
    @Override
    public OrderDetailResponseDto getOrderDetail(UUID orderId, Long userId) {
        // 1) 주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2) 조회 권한 검증 (구매자 또는 판매자)
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3) DTO 변환 및 반환
        return orderMapper.toDetailResponseDto(order);
    }

    // ✅ 주문 취소 (판매자/구매자 공용)
    @Override
    @Transactional
    public OrderDetailResponseDto cancelOrder(UUID orderId, Long userId) {
        // 1) 주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2) 권한 검증 (구매자 또는 판매자)
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 주문 취소 전 상태 저장
        OrderStatus previousStatus = order.getStatus();

        // 4) 주문 취소 처리
        order.cancel();

        // 5) 결제 전 취소: SellingBid 복구 (RESERVED → LIVE)
        if (previousStatus == OrderStatus.PAYMENT_PENDING) {
            tradeClient.liveSellingBid(order.getSellingBidId(), "ORDER_SERVICE");
            log.info("결제 전 주문 취소 - SellingBid 복구: {}", order.getSellingBidId());
        }

        // 6) 결제 후 취소: 환불 처리 필요 (향후 구현)
        if (previousStatus == OrderStatus.PENDING_SHIPMENT) {
            log.warn("결제 완료 후 주문 취소 - 환불 처리 필요: OrderID={}", orderId);
            // TODO: paymentService.refundPayment(orderId);
        }

        // 7) DTO 변환 및 반환
        return orderMapper.toDetailResponseDto(order);
    }

    // ✅ 운송장 번호 등록 (판매자용)
    @Override
    @Transactional
    public OrderDetailResponseDto registerTracking(UUID orderId, String trackingNumber, Long sellerId) {
        // 1) 주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2) 판매자 권한 검증
        if (!order.getSellerId().equals(sellerId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 운송장 등록 및 상태 변경
        order.registerTracking(trackingNumber);

        // 4) DTO 변환 및 반환
        return orderMapper.toDetailResponseDto(order);
    }

    // ✅ 구매 확정 (구매자 전용)
    @Override
    @Transactional
    public OrderDetailResponseDto confirmOrder(UUID orderId, Long userId) {
        // 1) 주문 조회
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2) 구매 확정 처리 (userId 기반)
        order.confirm(userId);

        // 3) 정산 확정 처리
        settlementService.confirmSettlement(orderId);

        // 4) DTO 변환 및 반환
        return orderMapper.toDetailResponseDto(order);
    }

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================

    // ✅ 주문 조회 (결제용)
    @Override
    @Transactional(readOnly = true)
    public OrderForPaymentInfoResponse getOrderForPayment(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        return orderClientMapper.toOrderForPaymentInfoResponse(order);
    }

    // ✅ 주문 조회 (리뷰용)
    @Override
    @Transactional(readOnly = true)
    public OrderForReviewInfoResponse getOrderForReview(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        return orderClientMapper.toOrderForReviewInfoResponse(order);
    }

    // ✅ 주문 상태 변경 (결제 완료용: PAYMENT_PENDING → PENDING_SHIPMENT)
    @Override
    @Transactional
    public void pendingShipmentOrder(UUID orderId, String updatedBy) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 상태 변경 (내부에서 PAYMENT_PENDING 검증)
        order.updateStatusAfterPayment();
    }
}