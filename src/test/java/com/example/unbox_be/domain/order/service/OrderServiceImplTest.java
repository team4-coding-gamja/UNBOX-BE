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
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.settlement.service.SettlementService;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
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
    private SettlementService settlementService;
    @Mock
    private OrderMapper orderMapper;


    private User createUser(Long id) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "email", "test" + id + "@test.com");
        return user;
    }

    private SellingBid createSellingBid(UUID id, Long sellerId, BigDecimal price, SellingStatus status) {
        SellingBid bid = BeanUtils.instantiateClass(SellingBid.class);
        
        Brand brand = BeanUtils.instantiateClass(Brand.class);
        Product product = BeanUtils.instantiateClass(Product.class);
        ReflectionTestUtils.setField(product, "brand", brand);
        ProductOption option = BeanUtils.instantiateClass(ProductOption.class);
        ReflectionTestUtils.setField(option, "product", product);

        ReflectionTestUtils.setField(bid, "id", id);
        ReflectionTestUtils.setField(bid, "userId", sellerId);
        ReflectionTestUtils.setField(bid, "productOption", option);
        ReflectionTestUtils.setField(bid, "price", price);
        ReflectionTestUtils.setField(bid, "status", status);
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

    // createOrder Tests

    @Test
    @DisplayName("주문 생성에 성공했을 때")
    void createOrder_Success() {
        Long buyerId = 1L;
        Long sellerId = 2L;
        UUID sellingBidId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal price = BigDecimal.valueOf(150000);

        User buyer = createUser(buyerId);
        User seller = createUser(sellerId);
        SellingBid sellingBid = createSellingBid(sellingBidId, sellerId, price, SellingStatus.LIVE);

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder()
                .sellingBidId(sellingBidId)
                .receiverName("수령인")
                .receiverPhone("010-1234-5678")
                .receiverAddress("서울")
                .receiverZipCode("12345")
                .build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
        given(sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(any(UUID.class)))
                .willReturn(Optional.of(sellingBid));
        given(userRepository.findByIdAndDeletedAtIsNull(sellerId)).willReturn(Optional.of(seller));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", orderId);
            return o;
        });

        UUID result = orderService.createOrder(requestDto, buyerId);

        assertThat(result).isEqualTo(orderId);
        assertThat(sellingBid.getStatus()).isEqualTo(SellingStatus.MATCHED);
    }

    @Test
    @DisplayName("구매자가 없어서 주문 생성에 실패했을 때")
    void createOrder_Fail_BuyerNotFound() {
        Long buyerId = 1L;
        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("판매 입찰 글이 없어서 주문 생성에 실패했을 때")
    void createOrder_Fail_BidNotFound() {
        Long buyerId = 1L;
        UUID sellingBidId = UUID.randomUUID();
        User buyer = createUser(buyerId);
        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().sellingBidId(sellingBidId).build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("판매 상품이 이미 판매된 상태라 주문 생성에 실패했을 때")
    void createOrder_Fail_BidStatusInvalid() {
        Long buyerId = 1L;
        Long sellerId = 2L;
        UUID sellingBidId = UUID.randomUUID();

        User buyer = createUser(buyerId);
        SellingBid sellingBid = createSellingBid(sellingBidId, sellerId, BigDecimal.valueOf(10000), SellingStatus.MATCHED);

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().sellingBidId(sellingBidId).build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
        given(sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(any(UUID.class)))
                .willReturn(Optional.of(sellingBid));

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("본인의 물건을 구매하려고 해서 주문 생성에 실패했을 때")
    void createOrder_Fail_SelfBuy() {
        Long buyerId = 1L;
        UUID sellingBidId = UUID.randomUUID();
        User buyer = createUser(buyerId);
        SellingBid sellingBid = createSellingBid(sellingBidId, buyerId, BigDecimal.valueOf(10000), SellingStatus.LIVE);

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().sellingBidId(sellingBidId).build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
        given(sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(any(UUID.class)))
                .willReturn(Optional.of(sellingBid));

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("판매자의 정보가 없어서(데이터가 불일치해서) 주문 생성에 실패했을 때")
    void createOrder_Fail_SellerUserNotFound() {
        Long buyerId = 1L;
        Long sellerId = 2L;
        UUID sellingBidId = UUID.randomUUID();

        User buyer = createUser(buyerId);
        SellingBid sellingBid = createSellingBid(sellingBidId, sellerId, BigDecimal.valueOf(10000), SellingStatus.LIVE);

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder().sellingBidId(sellingBidId).build();

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
        given(sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(any(UUID.class)))
                .willReturn(Optional.of(sellingBid));
        given(userRepository.findByIdAndDeletedAtIsNull(sellerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(requestDto, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    // getMyOrders Tests

    @Test
    @DisplayName("자기 주문 조회에 성공했을 때")
    void getMyOrders_Success() {
        Long buyerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Order order = createOrder(UUID.randomUUID(), createUser(buyerId), createUser(2L));
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        given(userRepository.existsById(buyerId)).willReturn(true);
        given(orderRepository.findAllByBuyerIdAndDeletedAtIsNull(buyerId, pageable)).willReturn(orderPage);
        given(orderMapper.toResponseDto(any(Order.class))).willReturn(OrderResponseDto.builder().build());

        Page<OrderResponseDto> result = orderService.getMyOrders(buyerId, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("유저가 없어서 내 주문 조회에 실패했을 때")
    void getMyOrders_Fail_UserNotFound() {
        Long buyerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        given(userRepository.existsById(buyerId)).willReturn(false);

        assertThatThrownBy(() -> orderService.getMyOrders(buyerId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    // getOrderDetail Tests

    @Test
    @DisplayName("구매자가 주문 상세 조회에 성공했을 때")
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
    @DisplayName("판매자가 주문 상세 조회에 성공했을 때")
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
    @DisplayName("제3자가 접근해서 주문 상세 조회에 실패했을 때")
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
    @DisplayName("주문이 없어서 상세 조회에 실패했을 때")
    void getOrderDetail_Fail_NotFound() {
        UUID orderId = UUID.randomUUID();
        Long userId = 1L;
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }


    // cancelOrder Tests

    @Test
    @DisplayName("주문 취소에 성공했을 때")
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
    @DisplayName("판매자가 주문을 취소하려고 해서 주문 취소에 실패했을 때")
    void cancelOrder_Fail_SellerCannotCancel() {
        UUID orderId = UUID.randomUUID();
        Long sellerId = 2L;
        Order order = createOrder(orderId, createUser(1L), createUser(sellerId));

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        Long hackerId = 999L;
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, hackerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("이미 배송된 경우, 주문 취소하려고 할 때")
    void cancelOrder_Fail_AlreadyShipped() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;
        Order order = createOrder(orderId, createUser(buyerId), createUser(2L));
        // Order 내부 로직: 배송중/배송완료 등 특정 상태에서는 취소 불가
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_BUYER);

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }

    // registerTracking Tests

    @Test   
    @DisplayName("운송장 등록에 성공했을 때")
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
    @DisplayName("구매자가 운송장(검수 센터로 물품 보내는)을 등록하려고 할 때")
    void registerTracking_Fail_NotSeller() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;
        Order order = createOrder(orderId, createUser(buyerId), createUser(2L));

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.registerTracking(orderId, "123", buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("주문 상태가 올바르지 않은데(이미 배송됨 등) 운송장을 등록하려고 할 때")
    void registerTracking_Fail_InvalidStatus() {
        UUID orderId = UUID.randomUUID();
        Long sellerId = 2L;
        Order order = createOrder(orderId, createUser(1L), createUser(sellerId));
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_CENTER); // 이미 보냄

        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.registerTracking(orderId, "123", sellerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    // updateAdminStatus Tests

    @Test
    @DisplayName("물품이 센터에 도착하고, 검수자가 검수 합격이라고 상태 변경할 때")
    void updateAdminStatus_Success_InspectionPassed() {
        UUID orderId = UUID.randomUUID();
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_INSPECTOR);

        Order order = createOrder(orderId, createUser(1L), createUser(2L));
        // 전제 상태: 센터 도착
        ReflectionTestUtils.setField(order, "status", OrderStatus.ARRIVED_AT_CENTER);

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        orderService.updateAdminStatus(orderId, OrderStatus.INSPECTION_PASSED, null, adminId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.INSPECTION_PASSED);
    }

    @Test
    @DisplayName("물품이 센터에 도착하고, 서비스 관리자가 검수 합격이라고 상태 변경하려고 하면 실패")
    void updateAdminStatus_Fail_InspectionPassed() {
        UUID orderId = UUID.randomUUID();
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_MANAGER);

        Order order = createOrder(orderId, createUser(1L), createUser(2L));
        // 전제 상태: 센터 도착
        ReflectionTestUtils.setField(order, "status", OrderStatus.ARRIVED_AT_CENTER);

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> orderService.updateAdminStatus(UUID.randomUUID(), OrderStatus.INSPECTION_PASSED, null, adminId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }


    @Test
    @DisplayName("물품이 센터에 도착하고, 검수자가 검수 불합격이라고 상태 변경할 때")
    void updateAdminStatus_Success_InspectionFailed() {
        UUID orderId = UUID.randomUUID();
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_INSPECTOR);

        Order order = createOrder(orderId, createUser(1L), createUser(2L));
        // 전제 상태: 센터 도착
        ReflectionTestUtils.setField(order, "status", OrderStatus.ARRIVED_AT_CENTER);

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        orderService.updateAdminStatus(orderId, OrderStatus.INSPECTION_FAILED, null, adminId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.INSPECTION_FAILED);
    }

    @Test
    @DisplayName("검수자가 최종 운송장을 입력하는 배송 시작 상태로 변경할 때")
    void updateAdminStatus_Success_ShippedToBuyer() {
        UUID orderId = UUID.randomUUID();
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_INSPECTOR);

        Order order = createOrder(orderId, createUser(1L), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.INSPECTION_PASSED);

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDetailResponseDto(order)).willReturn(OrderDetailResponseDto.builder().build());

        String finalTracking = "GTX-12345";
        orderService.updateAdminStatus(orderId, OrderStatus.SHIPPED_TO_BUYER, finalTracking, adminId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED_TO_BUYER);
        assertThat(order.getFinalTrackingNumber()).isEqualTo(finalTracking);
    }

    @Test
    @DisplayName("서비스 관리자가 상태를 변경하려고 할 때")
    void updateAdminStatus_Fail_RoleNotAllowed() {
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_MANAGER);

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> orderService.updateAdminStatus(UUID.randomUUID(), OrderStatus.ARRIVED_AT_CENTER, null, adminId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("사전 상태가 잘못되어서 검수자가 상태 변경에 실패했을 때")
    void updateAdminStatus_Fail_InvalidTransition() {
        UUID orderId = UUID.randomUUID();
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_INSPECTOR);

        Order order = createOrder(orderId, createUser(1L), createUser(2L));
        // 초기 상태에서 갑자기 배송 완료로 넘어갈 수 없음
        ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT);

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateAdminStatus(orderId, OrderStatus.DELIVERED, null, adminId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("최종 운송장이 누락되어서 관리자가 상태 변경에 실패했을 때")
    void updateAdminStatus_Fail_TrackingNumberMissing() {
        UUID orderId = UUID.randomUUID();
        Long adminId = 10L;
        Admin admin = BeanUtils.instantiateClass(Admin.class);
        ReflectionTestUtils.setField(admin, "adminRole", AdminRole.ROLE_MASTER);

        Order order = createOrder(orderId, createUser(1L), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.INSPECTION_PASSED);

        given(adminRepository.findByIdAndDeletedAtIsNull(adminId)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // 운송장 없이 배송 시작 시도
        assertThatThrownBy(() -> orderService.updateAdminStatus(orderId, OrderStatus.SHIPPED_TO_BUYER, null, adminId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRACKING_NUMBER_REQUIRED);
    }

    // confirmOrder Tests

    @Test
    @DisplayName("구매 확정에 성공했을 때")
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
        verify(settlementService).confirmSettlement(orderId);
    }

    @Test
    @DisplayName("유저가 없어서 구매 확정에 실패했을 때")
    void confirmOrder_Fail_UserNotFound() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirmOrder(orderId, buyerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("본인이 아니라서 구매 확정에 실패했을 때")
    void confirmOrder_Fail_AccessDenied() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;
        Long otherUserId = 2L; // 판매자 혹은 제3자

        User otherUser = createUser(otherUserId);
        Order order = createOrder(orderId, createUser(buyerId), createUser(3L)); // Buyer=1, Seller=3
        ReflectionTestUtils.setField(order, "status", OrderStatus.DELIVERED);

        given(userRepository.findByIdAndDeletedAtIsNull(otherUserId)).willReturn(Optional.of(otherUser));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(orderId, otherUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("배송 완료 상태가 아닌데(아직 배송 중인데) 구매 확정하려고 할 때")
    void confirmOrder_Fail_InvalidStatus() {
        UUID orderId = UUID.randomUUID();
        Long buyerId = 1L;
        Order order = createOrder(orderId, createUser(buyerId), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_BUYER); // 아직 배송중

        given(userRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(order.getBuyer()));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(orderId, buyerId))
                .isInstanceOf(CustomException.class) // CustomException(ErrorCode.INVALID_ORDER_STATUS)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }
}