package com.example.unbox_be.domain.order.service;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.mapper.OrderMapper;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private SellingBidRepository sellingBidRepository;
    @Mock
    private OrderMapper orderMapper;

    // --- Helper Methods ---

    private User createUser(Long id) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private SellingBid createSellingBid(UUID id, Long sellerId, int price) {
        SellingBid bid = BeanUtils.instantiateClass(SellingBid.class);
        ProductOption option = BeanUtils.instantiateClass(ProductOption.class);
        ReflectionTestUtils.setField(bid, "sellingId", id);
        ReflectionTestUtils.setField(bid, "userId", sellerId);
        ReflectionTestUtils.setField(bid, "productOption", option);
        ReflectionTestUtils.setField(bid, "price", price);
        return bid;
    }

    private Order createOrder(UUID id, User buyer, User seller) {
        Order order = Order.builder()
                .sellingBidId(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .productOption(BeanUtils.instantiateClass(ProductOption.class))
                .price(BigDecimal.valueOf(100000))
                .receiverName("name")
                .receiverPhone("phone")
                .receiverAddress("addr")
                .receiverZipCode("zip")
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    // --- 1. createOrder Tests ---

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        Long buyerId = 1L;
        Long sellerId = 2L;
        UUID sellingBidId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        int price = 150000;

        User buyer = createUser(buyerId);
        User seller = createUser(sellerId);
        SellingBid sellingBid = createSellingBid(sellingBidId, sellerId, price);

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder()
                .sellingBidId(sellingBidId)
                .receiverName("수령인")
                .receiverPhone("010-1234-5678")
                .receiverAddress("서울")
                .receiverZipCode("12345")
                .build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
        given(sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)).willReturn(Optional.of(sellingBid));
        given(userRepository.findByIdAndDeletedAtIsNull(sellerId)).willReturn(Optional.of(seller));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", orderId);
            return o;
        });

        UUID result = orderService.createOrder(requestDto, buyerId);

        assertThat(result).isEqualTo(orderId);
    }

    @Test
    @DisplayName("주문 생성 실패 - 구매자 없음")
    void createOrder_Fail_BuyerNotFound() {
        Long buyerId = 1L;
        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 생성 실패 - 판매 입찰글 없음")
    void createOrder_Fail_BidNotFound() {
        Long buyerId = 1L;
        UUID sellingBidId = UUID.randomUUID();
        User buyer = createUser(buyerId);
        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().sellingBidId(sellingBidId).build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
        given(sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 생성 실패 - 본인 물건 구매 시도")
    void createOrder_Fail_SelfBuy() {
        Long buyerId = 1L;
        UUID sellingBidId = UUID.randomUUID();
        User buyer = createUser(buyerId);
        SellingBid sellingBid = createSellingBid(sellingBidId, buyerId, 10000); // Seller == Buyer

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().sellingBidId(sellingBidId).build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
        given(sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)).willReturn(Optional.of(sellingBid));

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("주문 생성 실패 - 판매자 정보 없음 (데이터 불일치)")
    void createOrder_Fail_SellerUserNotFound() {
        Long buyerId = 1L;
        Long sellerId = 2L;
        UUID sellingBidId = UUID.randomUUID();

        User buyer = createUser(buyerId);
        SellingBid sellingBid = createSellingBid(sellingBidId, sellerId, 10000);

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().sellingBidId(sellingBidId).build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
        given(sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)).willReturn(Optional.of(sellingBid));
        given(userRepository.findByIdAndDeletedAtIsNull(sellerId)).willReturn(Optional.empty()); // Seller Not Found

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    // --- 2. getMyOrders Tests ---

    @Test
    @DisplayName("내 주문 조회 성공")
    void getMyOrders_Success() {
        Long buyerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Order order = createOrder(UUID.randomUUID(), createUser(buyerId), createUser(2L));
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        given(userRepository.existsById(buyerId)).willReturn(true);
        given(orderRepository.findAllByBuyerId(buyerId, pageable)).willReturn(orderPage);
        given(orderMapper.toResponseDto(any(Order.class))).willReturn(OrderResponseDto.builder().build());

        Page<OrderResponseDto> result = orderService.getMyOrders(buyerId, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("내 주문 조회 실패 - 유저 없음")
    void getMyOrders_Fail_UserNotFound() {
        Long buyerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        given(userRepository.existsById(buyerId)).willReturn(false);

        assertThatThrownBy(() -> orderService.getMyOrders(buyerId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    // --- 3. getOrderDetail Tests ---

    @Test
    @DisplayName("상세 조회 성공 - 구매자")
    void getOrderDetail_Success_Buyer() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;
        Order order = createOrder(orderId, createUser(buyerId), createUser(2L));

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        orderService.getOrderDetail(orderId, buyerId);
        verify(orderMapper).toDetailResponseDto(order);
    }

    @Test
    @DisplayName("상세 조회 성공 - 판매자")
    void getOrderDetail_Success_Seller() {
        UUID orderId = UUID.randomUUID();
        Long sellerId = 2L;
        Order order = createOrder(orderId, createUser(1L), createUser(sellerId));

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        orderService.getOrderDetail(orderId, sellerId);
        verify(orderMapper).toDetailResponseDto(order);
    }

    @Test
    @DisplayName("상세 조회 실패 - 제3자 접근")
    void getOrderDetail_Fail_AccessDenied() {
        UUID orderId = UUID.randomUUID();
        Long hackerId = 999L;
        Order order = createOrder(orderId, createUser(1L), createUser(2L));

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, hackerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("상세 조회 실패 - 주문 없음")
    void getOrderDetail_Fail_NotFound() {
        UUID orderId = UUID.randomUUID();
        Long userId = 1L;
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }


    // --- 4. cancelOrder Tests ---

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;
        Order order = createOrder(orderId, createUser(buyerId), createUser(2L));

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        orderService.cancelOrder(orderId, buyerId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 취소 실패 - 판매자는 취소 불가(AccessDenied)")
    void cancelOrder_Fail_SellerCannotCancel() {
        UUID orderId = UUID.randomUUID();
        Long sellerId = 2L;
        Order order = createOrder(orderId, createUser(1L), createUser(sellerId));

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // OrderServiceImpl.cancelOrder 로직상 buyerId와 일치하지 않으면 ACCESS_DENIED
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, sellerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    // --- 5. registerTracking Tests ---

    @Test
    @DisplayName("운송장 등록 성공 - 판매자")
    void registerTracking_Success() {
        UUID orderId = UUID.randomUUID();
        Long sellerId = 2L;
        Order order = createOrder(orderId, createUser(1L), createUser(sellerId));
        String trackingNum = "123456789";

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        orderService.registerTracking(orderId, trackingNum, sellerId);

        assertThat(order.getTrackingNumber()).isEqualTo(trackingNum);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED_TO_CENTER);
    }

    @Test
    @DisplayName("운송장 등록 실패 - 구매자가 시도")
    void registerTracking_Fail_NotSeller() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;
        Order order = createOrder(orderId, createUser(buyerId), createUser(2L));

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.registerTracking(orderId, "123", buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    // --- 6. updateAdminStatus Tests ---

    @Test
    @DisplayName("관리자 상태 변경 성공")
    void updateAdminStatus_Success() {
        UUID orderId = UUID.randomUUID();
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_MASTER);

        Order order = createOrder(orderId, createUser(1L), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_CENTER); // 상태 맞춰줌

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        orderService.updateAdminStatus(orderId, OrderStatus.ARRIVED_AT_CENTER, null, adminId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ARRIVED_AT_CENTER);
    }

    @Test
    @DisplayName("관리자 상태 변경 실패 - 권한 부족(MANAGER)")
    void updateAdminStatus_Fail_RoleNotAllowed() {
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_MANAGER); // MANAGER는 권한 없음

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> orderService.updateAdminStatus(UUID.randomUUID(), OrderStatus.ARRIVED_AT_CENTER, null, adminId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    // --- 7. confirmOrder Tests ---

    @Test
    @DisplayName("구매 확정 성공")
    void confirmOrder_Success() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;
        Order order = createOrder(orderId, createUser(buyerId), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.DELIVERED);

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(order.getBuyer()));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        orderService.confirmOrder(orderId, buyerId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("구매 확정 실패 - 유저 없음")
    void confirmOrder_Fail_UserNotFound() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirmOrder(orderId, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}