package com.example.unbox_order.order.application.service;

import com.example.unbox_order.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_order.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_order.common.client.trade.dto.SellingBidForOrderResponse;
import com.example.unbox_order.common.client.trade.TradeClient;
import com.example.unbox_order.common.client.user.UserClient;
import com.example.unbox_order.common.client.user.dto.UserInfoForOrderResponse;
import com.example.unbox_order.order.presentation.mapper.OrderClientMapper;
import com.example.unbox_order.order.presentation.dto.request.OrderCreateRequestDto;
import com.example.unbox_order.order.presentation.dto.response.OrderDetailResponseDto;
import com.example.unbox_order.order.presentation.dto.response.OrderResponseDto;
import com.example.unbox_order.order.domain.entity.Order;
import com.example.unbox_order.order.domain.entity.OrderStatus;
import com.example.unbox_order.order.presentation.mapper.OrderMapper;
import com.example.unbox_order.order.domain.repository.OrderRepository;
import com.example.unbox_order.settlement.application.service.SettlementService;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import java.util.Objects;
import java.util.UUID;

import com.example.unbox_common.event.order.OrderCancelledEvent;
import com.example.unbox_common.event.order.OrderConfirmedEvent;
import com.example.unbox_common.event.order.OrderRefundRequestedEvent;
import com.example.unbox_order.order.application.event.producer.OrderEventProducer;

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
    private final OrderEventProducer orderEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${order.payment-timeout-minutes:10}")
    private long paymentTimeoutMinutes;

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

        order = orderRepository.save(order);
        
        // 7) 결제 만료 타이머 설정 (Redis) - Standardized Key Naming Policy 적용
        // Key Format: order:expiration:{orderId}:{sellingBidId}
        String expirationKey = "order:expiration:" + order.getId() + ":" + order.getSellingBidId();
        
        try {
            // setIfAbsent 사용 (혹시 모를 키 중복 방지 및 원자성 확보)
            Boolean result = redisTemplate.opsForValue().setIfAbsent(expirationKey, "PENDING", Duration.ofMinutes(paymentTimeoutMinutes));
            if (!Boolean.TRUE.equals(result)) {
                log.error("Failed to set expiration key (already exists or error): {}", expirationKey);
                throw new IllegalStateException("Failed to set expiration key");
            }
        } catch (Exception e) {
            log.error("Failed to set expiration timer for order: {}. Rolling back transaction.", order.getId(), e);
            
            // 보상 트랜잭션: 이미 선점(RESERVED)된 입찰을 되돌려야 함 (분산 트랜잭션 보상)
            try {
                tradeClient.liveSellingBid(order.getSellingBidId(), "ORDER_ROLLBACK");
            } catch (Exception rollbackEx) {
                log.error("Failed to rollback SellingBid reservation for bid: {}. Data inconsistency risk!", order.getSellingBidId(), rollbackEx);
                // 이 로그는 모니터링 시스템에서 Critical Alert로 잡아야 함
            }
            
            // Redis 저장 실패 시 주문 생성 자체를 롤백 (데이터 정합성 보장)
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        
        log.info("Order created successfully. Expiration timer set for {} minutes. Key: {}", paymentTimeoutMinutes, expirationKey);

        return order.getId();
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

    // 5) 결제 전 취소: SellingBid 복구 (Async)
        if (previousStatus == OrderStatus.PAYMENT_PENDING) {
            // 변경: 동기 호출(tradeClient) 제거 -> 비동기 이벤트 발행
            // Transaction Commit 후 발행이 보장되어야 하나, 우선 간단히 여기서 발행
            // (실무에선 TransactionalEventListener 사용 권장)
            OrderCancelledEvent event = new OrderCancelledEvent(
                    order.getId(),
                    order.getSellingBidId(),
                    order.getBuyerId(),
                    order.getSellerId(),
                    "User Cancelled"
            );
            orderEventProducer.publishOrderCancelled(event);
        }

        // 6) 결제 완료된 주문은 cancelOrder가 아닌 requestRefund API 사용 안내
        // (Order.cancel() 내부에서 이미 예외 발생하지만, 명확한 안내 위해 추가)
        if (previousStatus == OrderStatus.PENDING_SHIPMENT 
                || previousStatus == OrderStatus.DELIVERED) {
            // 이 코드에 도달하지 않음 (Order.cancel()에서 예외 발생)
            // 단, Order.cancel()이 해당 상태를 허용하도록 변경될 경우 대비
            log.error("결제 완료된 주문 취소 시도 - requestRefund API 사용 필요: OrderID={}", orderId);
            throw new CustomException(ErrorCode.REFUND_REQUIRED_FOR_PAID_ORDER);
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

        // 3) 정산 확정 처리 (기존 동기 호출 유지 - 추후 제거 가능)
        settlementService.confirmSettlement(orderId);

        // 4) 구매 확정 이벤트 발행 (비동기 - 정산, 통계 서비스 등)
        orderEventProducer.publishOrderConfirmed(OrderConfirmedEvent.of(orderId, userId));

        // 5) DTO 변환 및 반환
        return orderMapper.toDetailResponseDto(order);
    }

    // ✅ 환불 요청 (결제 후, 구매자만)
    @Override
    @Transactional
    public OrderDetailResponseDto requestRefund(UUID orderId, String reason, Long userId) {
        // 1) 주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2) 환불 요청 처리 (상태 검증 + 본인 확인 + 상태 변경)
        OrderStatus previousStatus = order.requestRefund(userId);

        // 3) 환불 요청 이벤트 발행 (Payment → 환불 처리, Trade → 입찰 복구)
        OrderRefundRequestedEvent event = OrderRefundRequestedEvent.of(
                order.getId(),
                order.getSellingBidId(),
                order.getPaymentId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getPrice(),
                previousStatus.name(),
                reason
        );
        orderEventProducer.publishRefundRequested(event);

        log.info("Refund requested for Order {}: previousStatus={}, paymentId={}",
                orderId, previousStatus, order.getPaymentId());

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
    public void pendingShipmentOrder(UUID orderId, UUID paymentId, String updatedBy) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 상태 변경 (내부에서 PAYMENT_PENDING 검증) + paymentId 저장
        order.updateStatusAfterPayment(paymentId);
        
        // 🔄 Trade 서비스 상태 동기화 (RESERVED -> SOLD)
        // 비동기 이벤트(PaymentCompletedEvent)로 Trade 서비스에서 처리하므로 동기 호출 제거
        // tradeClient.soldSellingBid(order.getSellingBidId(), "ORDER_SERVICE");

        // 🟢 결제 완료 후 만료 타이머 제거 (불필요한 이벤트 발행 방지)
        String expirationKey = "order:expiration:" + orderId + ":" + order.getSellingBidId();
        try {
            Boolean deleted = redisTemplate.delete(expirationKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Deleted expiration timer for paid order: {}", orderId);
            } else {
                log.warn("Expiration key not found for paid order: {} (may have already expired)", orderId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete expiration timer for paid order: {}. Event may fire unnecessarily.", orderId, e);
        }
    }
    // ========================================
    // ✅ 검수 시스템 연동 (Inspection System Integration)
    // ========================================

    // ✅ 검수 시작 (ARRIVED_AT_CENTER -> IN_INSPECTION)
    @Override
    @Transactional
    public void startInspection(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        order.startInspection();
    }

    // ✅ 검수 합격 (IN_INSPECTION -> INSPECTION_PASSED)
    @Override
    @Transactional
    public void passedInspection(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        order.passedInspection();
    }

    // ✅ 검수 불합격 (IN_INSPECTION -> INSPECTION_FAILED)
    @Override
    @Transactional
    public void failedInspection(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        order.failedInspection();
    }
}