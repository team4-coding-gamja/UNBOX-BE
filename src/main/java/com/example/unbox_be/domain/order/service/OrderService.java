package com.example.unbox_be.domain.order.service;

import com.example.unbox_be.domain.order.dto.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.mapper.OrderMapper;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
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

    /**
     * 내 구매 내역 조회 (페이징)
     */
    public Page<OrderResponseDto> getMyOrders(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<Order> orderPage = orderRepository.findAllByBuyerId(user.getId(), pageable);

        return orderPage.map(orderMapper::toResponseDto);
    }

    /**
     * 주문 상세 조회
     * - N+1 방지를 위해 fetch join된 메서드 사용
     * - 본인의 주문(구매자 or 판매자)인지 권한 검증 포함
     */
    public OrderDetailResponseDto getOrderDetail(UUID orderId, String email) {
        // 1. 요청 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 주문 조회 (Repository에서 @EntityGraph로 연관 데이터 한 번에 로딩)
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 3. 권한 검증 (내 주문이 맞는지 확인)
        validateOrderAccess(order, user);

        // 4. 상세 조회용 DTO로 변환
        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 주문 취소 (판매자/구매자 공용)
     * - PATCH /api/orders/{orderId}/cancel
     */
    @Transactional
    public OrderDetailResponseDto cancelOrder(UUID orderId, String email) {
        // 1. 요청 사용자(판매자) 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 주문 조회 (권한 검증을 위해 fetch join 사용)
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 3. 권한 검증
        validateCancelRequest(order, user);

        // 4. 주문 취소 (Entity 비즈니스 로직 호출 -> 상태 변경 및 시간 기록)
        order.cancel();

        // 5. 변경된 결과 반환
        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 접근 권한 검증 메서드
     * - 요청한 사용자가 주문의 구매자(Buyer)이거나 판매자(Seller)여야 함
     */
    private void validateOrderAccess(Order order, User user) {
        // 방어적 코딩 (Buyer나 Seller가 없는 이상한 주문 데이터일 경우)
        if (order.getBuyer() == null || order.getSeller() == null) {
            throw new CustomException(ErrorCode.DATA_INTEGRITY_ERROR);
        }

        boolean isBuyer = order.getBuyer().getId().equals(user.getId());
        boolean isSeller = order.getSeller().getId().equals(user.getId());

        if (!isBuyer && !isSeller) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 취소 요청 유효성 검증
     * - 1차: 이 주문의 당사자(구매자/판매자)가 맞는지?
     * - 2차: 구매자라면 '배송 대기' 상태인지?
     */
    private void validateCancelRequest(Order order, User user) {
        // 1. 기본 접근 권한 체크
        validateOrderAccess(order, user);

        // 2. 구매자인 경우 상태 체크
        if (order.getBuyer().getId().equals(user.getId())) {
            // 구매자는 오직 'PENDING_SHIPMENT(배송 대기)' 상태일 때만 취소 가능
            if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
                throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
            }
        }
        // 판매자는 Order.cancel() 메서드에서 배송 상태 검증
    }

    /**
     * 운송장 번호 등록 (판매자용)
     * - PATCH /api/orders/{orderId}/tracking
     */
    @Transactional
    public OrderDetailResponseDto registerTrackingNumber(UUID orderId, String trackingNumber, String email) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 주문 조회 (권한 검증을 위해 Fetch Join된 메서드 사용)
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 3. 판매자 권한 검증 (판매자만 운송장 등록 가능)
        if (!order.getSeller().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 4. 운송장 등록 및 상태 변경
        order.registerTracking(trackingNumber);

        // 5. 결과 반환
        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 관리자/검수자 주문 상태 변경
     * - PATCH /api/orders/{orderId}/status
     */
    @Transactional
    public OrderDetailResponseDto updateOrderStatus(UUID orderId, OrderStatus newStatus, String trackingNumber, String email) {
        // 1. 관리자 조회 (Admin 테이블에서 조회)
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        // 2. 권한 검증
        // 마스터(MASTER), 검수자(INSPECTOR) 권한이 있는지 확인
        if (admin.getAdminRole() != AdminRole.ROLE_MASTER &&
                admin.getAdminRole() != AdminRole.ROLE_INSPECTOR) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 주문 조회
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 4. 상태 변경 (검증 로직이 포함된 Entity 메서드 호출)
        order.updateAdminStatus(newStatus, trackingNumber);

        // 5. 결과 반환
        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 구매 확정 (구매자 전용)
     * - PATCH /api/orders/{orderId}/confirm
     * - 배송 완료(DELIVERED) 상태일 때만 가능
     */
    @Transactional
    public OrderDetailResponseDto confirmOrder(UUID orderId, String email) {
        // 1. 요청 사용자(구매자) 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 주문 조회
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 3. 구매 확정 (Entity 내부 confirm 메서드 호출)
        order.confirm(user);

        // 4. 결과 반환
        return orderMapper.toDetailResponseDto(order);
    }
}