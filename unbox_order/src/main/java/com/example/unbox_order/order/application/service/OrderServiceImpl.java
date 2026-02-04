package com.example.unbox_order.order.application.service;

import com.example.unbox_common.event.order.OrderShipmentExpiredEvent;
import com.example.unbox_order.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_order.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_order.common.client.trade.dto.BuyingBidForOrderResponse;
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

    @Value("${order.shipment-timeout-days:1}") // 기본 1일
    private long shipmentTimeoutDays;

    // 상수로 관리하는 것이 좋으므로 클래스 상단이나 별도 상수 클래스에 정의 권장
    private static final String REDIS_SHIPMENT_KEY_PREFIX = "order:shipment-deadline:";

    // ✅ 주문 생성 (판매 입찰 구매 OR 구매 입찰 판매)
    @Override
    @Transactional
    public UUID createOrder(OrderCreateRequestDto requestDto, Long buyerId) {
        Order order;
        UUID bidIdForRollback;
        boolean isBuyingBidTrade = false;

        // CASE 1: 판매 입찰 기반 주문 (User = Buyer, Target = SellingBid)
        if (requestDto.getSellingBidId() != null) {
            bidIdForRollback = requestDto.getSellingBidId();

            // 1) 구매자(Caller) 조회
            UserInfoForOrderResponse buyer = userClient.getUserInfoForOrder(buyerId);

            // 2) 판매 입찰 정보 조회
            SellingBidForOrderResponse sellingBidInfo = tradeClient.getSellingBidForOrder(requestDto.getSellingBidId());

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

            // 6) 주문 객체 생성
            order = Order.builder()
                    .sellingBidId(sellingBidInfo.getSellingBidId())
                    .buyerId(buyerId)
                    .sellerId(sellingBidInfo.getSellerId())
                    .buyerName(buyer.getNickname())
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
        }
        // CASE 2: 구매 입찰 기반 주문 (User = Seller, Target = BuyingBid)
        else if (requestDto.getBuyingBidId() != null) {
            isBuyingBidTrade = true;
            bidIdForRollback = requestDto.getBuyingBidId();

            // 1) 구매 입찰 정보 조회
            BuyingBidForOrderResponse buyingBidInfo = tradeClient.getBuyingBidForOrder(requestDto.getBuyingBidId());

            // 2) 실제 구매자(Bidder) 조회 (Snapshot용)
            UserInfoForOrderResponse buyer = userClient.getUserInfoForOrder(buyingBidInfo.getBuyerId());

            // 3) 자전 거래 방지 (Caller = Seller)
            if (Objects.equals(buyingBidInfo.getBuyerId(), buyerId)) {
                throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
            }

            // 4) 구매 입찰 선점 (LIVE → RESERVED)
            tradeClient.reserveBuyingBid(buyingBidInfo.getBuyingBidId(), "ORDER_SERVICE");

            // 5) 주문 객체 생성
            order = Order.builder()
                    .buyingBidId(buyingBidInfo.getBuyingBidId())
                    .buyerId(buyingBidInfo.getBuyerId()) // Bidder is Buyer
                    .sellerId(buyerId) // Caller is Seller
                    .buyerName(buyer.getNickname())
                    .productOptionId(buyingBidInfo.getProductOptionId())
                    .productId(buyingBidInfo.getProductId())
                    .productName(buyingBidInfo.getProductName())
                    .modelNumber(buyingBidInfo.getModelNumber())
                    .productOptionName(buyingBidInfo.getProductOptionName())
                    .productImageUrl(buyingBidInfo.getProductImageUrl())
                    .brandName(buyingBidInfo.getBrandName())
                    .price(buyingBidInfo.getPrice())
                    .receiverName(requestDto.getReceiverName())
                    .receiverPhone(requestDto.getReceiverPhone())
                    .receiverAddress(requestDto.getReceiverAddress())
                    .receiverZipCode(requestDto.getReceiverZipCode())
                    .build();
        } else {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        order = orderRepository.save(order);

        // 7) 결제 만료 타이머 설정 (Redis)
        String type = isBuyingBidTrade ? "BUYING" : "SELLING";
        String expirationKey = "order:expiration:" + order.getId() + ":" + type + ":" + bidIdForRollback;

        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(expirationKey, "PENDING",
                    Duration.ofMinutes(paymentTimeoutMinutes));
            if (!Boolean.TRUE.equals(result)) {
                log.error("Failed to set expiration key (already exists or error): {}", expirationKey);
                throw new IllegalStateException("Failed to set expiration key");
            }
        } catch (Exception e) {
            log.error("Failed to set expiration timer for order: {}. Rolling back transaction.", order.getId(), e);

            // 보상 트랜잭션: 선점된 입찰 복구
            try {
                if (isBuyingBidTrade) {
                    tradeClient.liveBuyingBid(bidIdForRollback, "ORDER_ROLLBACK");
                } else {
                    tradeClient.liveSellingBid(bidIdForRollback, "ORDER_ROLLBACK");
                }
            } catch (Exception rollbackEx) {
                log.error("Failed to rollback bid reservation for bid: {}. Data inconsistency risk!", bidIdForRollback,
                        rollbackEx);
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        log.info("Order created successfully. Expiration timer set for {} minutes. Key: {}", paymentTimeoutMinutes,
                expirationKey);

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
            OrderCancelledEvent event = OrderCancelledEvent.of(
                    order.getId(),
                    order.getSellingBidId(),
                    order.getBuyingBidId(),
                    order.getBuyerId(),
                    order.getSellerId(),
                    "User Cancelled");
            orderEventProducer.publishOrderCancelled(event);
        }

        // 6) 결제 완료된 주문은 cancelOrder가 아닌 requestRefund API 사용 안내
        if (previousStatus == OrderStatus.PENDING_SHIPMENT
                || previousStatus == OrderStatus.DELIVERED) {
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

        // 4) [추가] 배송 기한 만료 타이머 제거 (Redis Key 삭제)
        String shipmentDeadlineKey = REDIS_SHIPMENT_KEY_PREFIX + orderId;
        try {
            Boolean deleted = redisTemplate.delete(shipmentDeadlineKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Deleted shipment deadline timer for Order: {}", orderId);
            } else {
                // 이미 만료되었거나 키가 없는 경우 (큰 문제 아님)
                log.debug("Shipment deadline timer not found for Order: {}", orderId);
            }
        } catch (Exception e) {
            // Redis 에러가 나더라도 운송장 등록 로직 자체(DB 트랜잭션)는 성공해야 하므로 로그만 남김
            log.warn("Failed to delete shipment timer for Order: {}. Event may fire unnecessarily.", orderId, e);
        }

        // 5) DTO 변환 및 반환
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
                order.getBuyingBidId(),
                order.getPaymentId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getPrice(),
                previousStatus.name(),
                reason);
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

        // 3. [추가] 배송 기한 타이머 설정 (Redis Shadow Key)
        // Key 예시: "order:shipment-deadline:{orderId}"
        String shipmentDeadlineKey = REDIS_SHIPMENT_KEY_PREFIX + orderId;
        try {
            redisTemplate.opsForValue().set(
                    shipmentDeadlineKey,
                    "PENDING",
                    Duration.ofDays(shipmentTimeoutDays) // 예: 2일 뒤 만료
            );
            log.info("Set shipment deadline for Order {}: {} days", orderId, shipmentTimeoutDays);
        } catch (Exception e) {
            log.error("Failed to set shipment timer for order: {}", orderId, e);
            // 중요: 여기서 에러가 나도 트랜잭션을 롤백할지, 알람만 보낼지 결정 필요 (보통 알람 후 수동 처리 권장)
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

    @Override
    @Transactional
    public void processShipmentOverdue(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 이미 배송했거나 취소된 건인지 동시성 검증
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            log.warn("Shipment timeout trigger ignored. Order {} is status {}", orderId, order.getStatus());
            return;
        }

        // 1. 주문 상태 변경 (PENDING_SHIPMENT -> CANCELLED)
        order.cancel();

        // 2. 이벤트 객체 생성 (SHIPMENT_CANCELLED)
        // 필요한 모든 서비스가 처리할 수 있도록 충분한 정보를 담습니다.
        OrderShipmentExpiredEvent event = OrderShipmentExpiredEvent.of(
                order.getId(),
                order.getSellingBidId(), // Trade: 판매 입찰 취소/페널티용
                order.getBuyingBidId(),
                order.getPaymentId(),    // Payment: 환불용
                order.getBuyerId(),
                order.getSellerId(),     // Settlement: 페널티 부과 대상
                order.getPrice()
        );

        // 3. [핵심] 이벤트 발행
        // 각 서비스(Trade, Payment, Settlement)가 이 토픽을 구독합니다.
        orderEventProducer.publishShipmentExpired(event);

        log.info("Processed shipment overdue for Order {}. Event published.", orderId);
    }

}