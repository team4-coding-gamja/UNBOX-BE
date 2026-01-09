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

    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public UUID createOrder(OrderCreateRequestDto requestDto, Long buyerId) {
        // 1. 구매자 조회
        User buyer = getUserByIdOrThrow(buyerId);

        // 2. 판매 입찰 글(SellingBid) 조회
        // SellingBid ID는 UUID이므로 DTO도 UUID여야 함
        // 2. 판매 입찰 글(SellingBid) 조회 (비관적 락 적용)
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(requestDto.getSellingBidId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (sellingBid.getStatus() == SellingStatus.MATCHED || sellingBid.getStatus() == SellingStatus.HOLD) {
            // ErrorCode.ALREADY_SOLD_PRODUCT 가 없다면 PRODUCT_NOT_FOUND 혹은 별도 에러코드 사용
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 3. 본인 판매글 구매 방지
        // sellingBid.getUserId()는 Long 타입이므로 바로 비교 가능
        if (sellingBid.getUserId().equals(buyerId)) {
            // "자신의 판매글은 구매할 수 없습니다"
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 4. 판매자 정보 조회 (SellingBid에는 ID만 있으므로 조회 필요)
        User seller = getUserByIdOrThrow(sellingBid.getUserId());

        // 5. 가격 타입 변환 (Integer -> BigDecimal)
        BigDecimal price = sellingBid.getPrice();

        // 6. Order 생성
        Order order = Order.builder()
                .sellingBidId(sellingBid.getId())
                .buyer(buyer)
                .seller(seller)          // 위에서 조회한 User 객체 주입
                .productOption(sellingBid.getProductOption())
                .price(price)            // 변환된 BigDecimal 주입
                .receiverName(requestDto.getReceiverName())
                .receiverPhone(requestDto.getReceiverPhone())
                .receiverAddress(requestDto.getReceiverAddress())
                .receiverZipCode(requestDto.getReceiverZipCode())
                .build();

        // 7. 판매 입찰 상태 변경 (판매 완료 처리)
        // SellingBid 엔티티에 updateStatus 메서드가 있으므로 활용
        sellingBid.updateStatus(SellingStatus.MATCHED);

        return orderRepository.save(order).getId();
    }

    /**
     * 내 구매 내역 조회 (페이징)
     */
    @Override
    public Page<OrderResponseDto> getMyOrders(Long buyerId, Pageable pageable) {
        // ID 검증 (존재하는 유저인지)
        if (!userRepository.existsById(buyerId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // Repository에서 EntityGraph 등을 활용해 조회
        return orderRepository.findAllByBuyerIdAndDeletedAtIsNull(buyerId, pageable)
                .map(orderMapper::toResponseDto); // Static Method Reference
    }

    /**
     * 주문 상세 조회
     */
    @Override
    public OrderDetailResponseDto getOrderDetail(UUID orderId, Long userId) {
        Order order = getOrderWithDetailsOrThrow(orderId);
        // MVP: 관리자 로직 제외하고 본인(구매자/판매자)만 조회 가능하도록 유지
        validateOrderReadAccess(order, userId);
        return orderMapper.toDetailResponseDto(order);
    }

    // API 명세와 구현체 메서드 시그니처 일치를 위해 String 버전이 혹시 남아있다면 제거하거나 오버로딩 해야함.
    // 여기서는 Controller가 String을 넘기던 것을 Long으로 수정했다고 가정하고 Long만 구현함.

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

        // 도메인 로직 호출 (Entity 내부에서 상태별 취소 가능 여부 검증)
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

        // 1. 판매자 본인 확인
        if (!order.getSeller().getId().equals(sellerId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 2. 도메인 로직 호출
        order.registerTracking(trackingNumber);

        return orderMapper.toDetailResponseDto(order);
    }

    /**
     * 관리자/검수자 주문 상태 변경
     * - 메서드 이름 불일치 문제 해결: updateAdminStatus
     */
    @Override
    @Transactional
    public OrderDetailResponseDto updateAdminStatus(UUID orderId, OrderStatus newStatus, String finalTrackingNumber, Long adminId) {
        // 1. 관리자 조회
        Admin admin = adminRepository.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        // 2. 권한 검증 (MASTER, INSPECTOR만 가능)
        if (admin.getAdminRole() != AdminRole.ROLE_MASTER && admin.getAdminRole() != AdminRole.ROLE_INSPECTOR) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 주문 조회
        Order order = getOrderWithDetailsOrThrow(orderId);

        // 4. 도메인 로직 호출 (Entity 내부에서 상태 전이 검증 수행)
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

        // 도메인 로직 호출 (Entity 내부에서 구매자 ID 일치 여부 및 상태 검증)
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
            // 관리자 로직이 별도로 없다면 예외 처리
            // 실제 서비스에선 관리자(Admin) 호출인 경우 패스하는 로직이 필요할 수 있음
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }
}