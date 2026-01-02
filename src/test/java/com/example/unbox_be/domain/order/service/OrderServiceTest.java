package com.example.unbox_be.domain.order.service;

import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.entity.AdminRole;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.domain.order.dto.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.mapper.OrderMapper;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;

    // OrderService에 추가된 의존성 Mocking (주입 에러 방지)
    @Mock
    private SellingBidService sellingBidService;
    @Mock
    private AdminRepository adminRepository;

    @Spy
    private OrderMapper orderMapper;

    // --- Helper 메서드 (Entity 정적 팩토리 메서드 대응) ---
    private Brand createBrand() {
        return Brand.createBrand("Nike", "https://logo.com/nike.png");
    }

    private Product createProduct(Brand brand) {
        return Product.createProduct(
                "Air Force 1",
                "CW2288-111",
                Category.SHOES,
                "https://image.com/af1.png",
                brand
        );
    }

    @Test
    @DisplayName("주문 생성 성공 테스트")
    void createOrder_Success() {
        // Given
        Long sellerId = 2L;
        String buyerEmail = "buyer@test.com";
        UUID optionId = UUID.randomUUID();
        BigDecimal price = BigDecimal.valueOf(300000);

        // Builder 패턴 사용
        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder()
                .sellerId(sellerId)
                .productOptionId(optionId)
                .price(price)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .receiverAddress("서울")
                .receiverZipCode("12345")
                .build();

        // Mock 데이터 생성
        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");

        // Helper 메서드로 객체 생성
        Brand brand = createBrand();
        Product product = createProduct(brand);
        ProductOption option = new ProductOption(product, "270");

        // 가짜 행동 정의
        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(userRepository.findById(sellerId)).willReturn(Optional.of(seller));
        given(productOptionRepository.findById(optionId)).willReturn(Optional.of(option));

        Order savedOrder = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(price)
                .receiverName("홍길동")
                .build();
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // When
        OrderResponseDto result = orderService.createOrder(requestDto, buyerEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualTo(price);
        assertThat(result.getBrandName()).isEqualTo("Nike");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 실패 - 존재하지 않는 구매자")
    void createOrder_Fail_UserNotFound() {
        // Given
        Long sellerId = 2L;
        String unknownEmail = "unknown@test.com";
        UUID optionId = UUID.randomUUID();

        // Builder 사용
        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder()
                .sellerId(sellerId)
                .productOptionId(optionId)
                .price(BigDecimal.valueOf(300000))
                .receiverName("수령인")
                .receiverPhone("010-0000-0000")
                .receiverAddress("주소")
                .receiverZipCode("12345")
                .build();

        given(userRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                orderService.createOrder(requestDto, unknownEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 실패 - 존재하지 않는 판매자")
    void createOrder_Fail_SellerNotFound() {
        // Given
        Long unknownSellerId = 999L;
        String buyerEmail = "buyer@test.com";
        UUID optionId = UUID.randomUUID();

        // Builder 사용
        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder()
                .sellerId(unknownSellerId)
                .productOptionId(optionId)
                .price(BigDecimal.valueOf(300000))
                .receiverName("수령인")
                .receiverPhone("010-0000-0000")
                .receiverAddress("주소")
                .receiverZipCode("12345")
                .build();

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111")));
        given(userRepository.findById(unknownSellerId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                orderService.createOrder(requestDto, buyerEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 실패 - 존재하지 않는 상품 옵션")
    void createOrder_Fail_OptionNotFound() {
        // Given
        Long sellerId = 2L;
        String buyerEmail = "buyer@test.com";
        UUID unknownOptionId = UUID.randomUUID();

        // Builder 사용
        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder()
                .sellerId(sellerId)
                .productOptionId(unknownOptionId)
                .price(BigDecimal.valueOf(300000))
                .build();

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111")));
        given(userRepository.findById(sellerId)).willReturn(Optional.of(User.createUser("seller", "pw", "seller", "010-2222-2222")));
        given(productOptionRepository.findById(unknownOptionId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                orderService.createOrder(requestDto, buyerEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("내 구매 내역 조회 성공 테스트")
    void getMyOrders_Success() {
        // Given
        String email = "buyer@test.com";
        User buyer = User.createUser(email, "pw", "buyer", "010-1111-1111");
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(buyer));

        List<Order> orders = List.of(
                Order.builder().price(BigDecimal.valueOf(10000)).build(),
                Order.builder().price(BigDecimal.valueOf(20000)).build()
        );
        Page<Order> orderPage = new PageImpl<>(orders);

        given(orderRepository.findAllByBuyerId(eq(buyer.getId()), eq(pageable))).willReturn(orderPage);

        doReturn(
                OrderResponseDto.builder()
                        .price(BigDecimal.valueOf(10000))
                        .status(OrderStatus.PENDING_SHIPMENT)
                        .build()
        ).when(orderMapper).toResponseDto(any(Order.class));

        // When
        Page<OrderResponseDto> result = orderService.getMyOrders(email, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(orderRepository).findAllByBuyerId(any(), any());
    }

    @Test
    @DisplayName("내 구매 내역 조회 실패 - 존재하지 않는 구매자")
    void getMyOrders_Fail_UserNotFound() {
        // Given
        String unknownEmail = "unknown@test.com";
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                orderService.getMyOrders(unknownEmail, pageable)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 상세 조회 성공 - 구매자가 조회 시")
    void getOrderDetail_Success_Buyer() {
        // Given
        UUID orderId = UUID.randomUUID();
        String buyerEmail = "buyer@test.com";

        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);

        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        // Helper 메서드 사용
        Brand brand = createBrand();
        Product product = createProduct(brand);
        ProductOption option = new ProductOption(product, "270");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .receiverName("홍길동")
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When
        OrderDetailResponseDto result = orderService.getOrderDetail(orderId, buyerEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(300000));
    }

    @Test
    @DisplayName("주문 상세 조회 실패 - 권한 없는 사용자(제3자)")
    void getOrderDetail_Fail_AccessDenied() {
        // Given
        UUID orderId = UUID.randomUUID();
        String hackerEmail = "hacker@test.com";

        User hacker = User.createUser(hackerEmail, "pw", "hacker", "010-9999-9999");
        ReflectionTestUtils.setField(hacker, "id", 3L);

        User buyer = User.createUser("buyer@test.com", "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        Order order = Order.builder().buyer(buyer).seller(seller).price(BigDecimal.ZERO).build();

        given(userRepository.findByEmail(hackerEmail)).willReturn(Optional.of(hacker));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, hackerEmail))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("주문 상세 조회 실패 - 주문이 존재하지 않음")
    void getOrderDetail_Fail_OrderNotFound() {
        // Given
        UUID orderId = UUID.randomUUID();
        String email = "user@test.com";
        User user = User.createUser(email, "pw", "user", "010-1111-1111");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, email))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 상세 조회 실패 - 데이터 무결성 오류 (Buyer/Seller 누락)")
    void getOrderDetail_Fail_DataIntegrity() {
        // Given
        UUID orderId = UUID.randomUUID();
        String email = "user@test.com";
        User user = User.createUser(email, "pw", "user", "010-1111-1111");

        Order brokenOrder = Order.builder()
                .buyer(null)
                .seller(null)
                .price(BigDecimal.ZERO)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(brokenOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, email))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_ERROR);
    }

    @Test
    @DisplayName("주문 상세 조회 실패 - 데이터 무결성 오류 (Seller만 누락된 경우)")
    void getOrderDetail_Fail_DataIntegrity_SellerMissing() {
        // Given
        UUID orderId = UUID.randomUUID();
        String email = "user@test.com";
        User user = User.createUser(email, "pw", "user", "010-1111-1111");

        Order brokenOrder = Order.builder()
                .buyer(user)
                .seller(null)
                .price(BigDecimal.ZERO)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(brokenOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, email))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_ERROR);
    }

    @Test
    @DisplayName("주문 상세 조회 실패 - 존재하지 않는 사용자")
    void getOrderDetail_Fail_UserNotFound() {
        // Given
        UUID orderId = UUID.randomUUID();
        String unknownEmail = "unknown@test.com";

        given(userRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, unknownEmail))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 취소 성공 - 구매자가 배송 대기 상태에서 취소")
    void cancelOrder_Success_Buyer() {
        // Given
        UUID orderId = UUID.randomUUID();
        String buyerEmail = "buyer@test.com";

        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);

        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        // Helper 메서드 사용
        Brand brand = createBrand();
        Product product = createProduct(brand);
        ProductOption option = new ProductOption(product, "270");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .build();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_SHIPMENT);

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When
        orderService.cancelOrder(orderId, buyerEmail);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("주문 취소 성공 - 판매자가 취소 요청")
    void cancelOrder_Success_Seller() {
        // Given
        UUID orderId = UUID.randomUUID();
        String sellerEmail = "seller@test.com";

        User seller = User.createUser(sellerEmail, "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        User buyer = User.createUser("buyer@test.com", "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);

        // Helper 메서드 사용
        Brand brand = createBrand();
        Product product = createProduct(brand);
        ProductOption option = new ProductOption(product, "270");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .build();

        given(userRepository.findByEmail(sellerEmail)).willReturn(Optional.of(seller));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When
        orderService.cancelOrder(orderId, sellerEmail);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("주문 취소 실패 - 존재하지 않는 사용자")
    void cancelOrder_Fail_UserNotFound() {
        // Given
        UUID orderId = UUID.randomUUID();
        String unknownEmail = "unknown@test.com";

        given(userRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, unknownEmail))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 취소 실패 - 존재하지 않는 주문")
    void cancelOrder_Fail_OrderNotFound() {
        // Given
        UUID orderId = UUID.randomUUID();
        String email = "seller@test.com";
        User user = User.createUser(email, "pw", "seller", "010-2222-2222");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        // 주문을 찾지 못함
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, email))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 취소 실패 - 권한 없는 사용자(제3자)")
    void cancelOrder_Fail_AccessDenied() {
        // Given
        UUID orderId = UUID.randomUUID();
        String hackerEmail = "hacker@test.com";

        User hacker = User.createUser(hackerEmail, "pw", "hacker", "010-3333-3333");
        ReflectionTestUtils.setField(hacker, "id", 3L);

        User buyer = User.createUser("buyer@test.com", "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        // 구매자 1, 판매자 2의 주문
        Order order = Order.builder().buyer(buyer).seller(seller).price(BigDecimal.TEN).build();

        given(userRepository.findByEmail(hackerEmail)).willReturn(Optional.of(hacker));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, hackerEmail))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("주문 취소 실패 - 구매자가 배송 시작(SHIPPED) 이후 취소 시도")
    void cancelOrder_Fail_Buyer_AlreadyShipped() {
        // Given
        UUID orderId = UUID.randomUUID();
        String buyerEmail = "buyer@test.com";

        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .price(BigDecimal.valueOf(300000))
                .build();

        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_CENTER);

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, buyerEmail))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }

    @Test
    @DisplayName("운송장 등록 성공 - 판매자가 등록 요청 시 상태가 변경된다")
    void registerTrackingNumber_Success() {
        UUID orderId = UUID.randomUUID();
        String sellerEmail = "seller@test.com";
        String trackingNumber = "1234567890";

        User seller = User.createUser(sellerEmail, "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);
        User buyer = User.createUser("buyer@test.com", "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);

        Brand brand = createBrand();
        Product product = createProduct(brand);
        ProductOption option = new ProductOption(product, "270");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .build();

        given(userRepository.findByEmail(sellerEmail)).willReturn(Optional.of(seller));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        OrderDetailResponseDto result = orderService.registerTrackingNumber(orderId, trackingNumber, sellerEmail);

        assertThat(order.getTrackingNumber()).isEqualTo(trackingNumber);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED_TO_CENTER);
    }

    @Test
    @DisplayName("운송장 등록 실패 - 권한 없는 사용자(구매자)")
    void registerTrackingNumber_Fail_AccessDenied() {
        // Given
        UUID orderId = UUID.randomUUID();
        String buyerEmail = "buyer@test.com"; // 구매자가 시도
        String trackingNumber = "1234567890";

        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .price(BigDecimal.valueOf(300000))
                .build();

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() ->
                orderService.registerTrackingNumber(orderId, trackingNumber, buyerEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("검수자 주문 상태 변경 성공 - 센터 도착 처리")
    void updateOrderStatus_Success_ArrivedAtCenter() {
        UUID orderId = UUID.randomUUID();
        String adminEmail = "admin@test.com";
        OrderStatus newStatus = OrderStatus.ARRIVED_AT_CENTER;

        Admin admin = org.mockito.Mockito.mock(Admin.class);
        given(admin.getAdminRole()).willReturn(AdminRole.ROLE_INSPECTOR);

        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        User buyer = User.createUser("buyer@test.com", "pw", "buyer", "010-1111-1111");

        Brand brand = createBrand();
        Product product = createProduct(brand);
        ProductOption option = new ProductOption(product, "270");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .build();

        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_CENTER);

        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        orderService.updateOrderStatus(orderId, newStatus, null, adminEmail);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ARRIVED_AT_CENTER);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 성공 - 구매자 발송 (운송장 포함)")
    void updateOrderStatus_Success_ShippedToBuyer() {
        UUID orderId = UUID.randomUUID();
        String adminEmail = "admin@test.com";
        String finalTrackingNumber = "FINAL-12345";
        OrderStatus newStatus = OrderStatus.SHIPPED_TO_BUYER;

        Admin admin = org.mockito.Mockito.mock(Admin.class);
        given(admin.getAdminRole()).willReturn(AdminRole.ROLE_MASTER);

        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        User buyer = User.createUser("buyer@test.com", "pw", "buyer", "010-1111-1111");

        Brand brand = createBrand();
        Product product = createProduct(brand);
        ProductOption option = new ProductOption(product, "270");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .build();

        ReflectionTestUtils.setField(order, "status", OrderStatus.INSPECTION_PASSED);

        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        orderService.updateOrderStatus(orderId, newStatus, finalTrackingNumber, adminEmail);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED_TO_BUYER);
        assertThat(order.getFinalTrackingNumber()).isEqualTo(finalTrackingNumber);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 실패 - 상태 흐름이 올바르지 않음")
    void updateOrderStatus_Fail_InvalidTransition() {
        // Given
        UUID orderId = UUID.randomUUID();
        String adminEmail = "admin@test.com";

        // PENDING_SHIPMENT 상태에서 갑자기 DELIVERED로 변경 시도
        OrderStatus currentStatus = OrderStatus.PENDING_SHIPMENT;
        OrderStatus invalidTargetStatus = OrderStatus.DELIVERED;

        Admin admin = org.mockito.Mockito.mock(Admin.class);
        given(admin.getAdminRole()).willReturn(AdminRole.ROLE_MASTER);

        Order order = Order.builder().price(BigDecimal.valueOf(300000)).build();
        ReflectionTestUtils.setField(order, "status", currentStatus);

        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() ->
                orderService.updateOrderStatus(orderId, invalidTargetStatus, null, adminEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 실패 - 구매자 발송 시 운송장 누락")
    void updateOrderStatus_Fail_MissingTrackingNumber() {
        // Given
        UUID orderId = UUID.randomUUID();
        String adminEmail = "admin@test.com";

        Admin admin = org.mockito.Mockito.mock(Admin.class);
        given(admin.getAdminRole()).willReturn(AdminRole.ROLE_MASTER);

        Order order = Order.builder().price(BigDecimal.valueOf(300000)).build();
        // 검수 통과 상태 설정
        ReflectionTestUtils.setField(order, "status", OrderStatus.INSPECTION_PASSED);

        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When & Then
        // 운송장 번호를 null로 전달
        assertThatThrownBy(() ->
                orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED_TO_BUYER, null, adminEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRACKING_NUMBER_REQUIRED);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 실패 - 권한 부족(매니저)")
    void updateOrderStatus_Fail_ManagerAccessDenied() {
        // Given
        UUID orderId = UUID.randomUUID();
        String adminEmail = "manager@test.com";

        Admin admin = org.mockito.Mockito.mock(Admin.class);
        // ROLE_MANAGER는 상태 변경 권한이 없다고 가정 (Service 로직 기준)
        given(admin.getAdminRole()).willReturn(AdminRole.ROLE_MANAGER);

        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin));

        // When & Then
        assertThatThrownBy(() ->
                orderService.updateOrderStatus(orderId, OrderStatus.INSPECTION_PASSED, null, adminEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("구매 확정 성공 - 배송 완료 상태에서만 가능하다")
    void confirmOrder_Success() {
        UUID orderId = UUID.randomUUID();
        String buyerEmail = "buyer@test.com";

        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        Brand brand = createBrand();
        Product product = createProduct(brand);
        ProductOption option = new ProductOption(product, "270");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .build();

        ReflectionTestUtils.setField(order, "status", OrderStatus.DELIVERED);

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        orderService.confirmOrder(orderId, buyerEmail);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("구매 확정 실패 - 배송 완료 전에는 확정 불가")
    void confirmOrder_Fail_InvalidStatus() {
        // Given
        UUID orderId = UUID.randomUUID();
        String buyerEmail = "buyer@test.com";

        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .price(BigDecimal.valueOf(300000))
                .build();

        // 배송 중 상태 (SHIPPED_TO_BUYER) 설정
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_BUYER);

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() ->
                orderService.confirmOrder(orderId, buyerEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("운송장 등록 실패 - 존재하지 않는 사용자")
    void registerTrackingNumber_Fail_UserNotFound() {
        UUID orderId = UUID.randomUUID();
        String unknownEmail = "unknown@test.com";

        given(userRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.registerTrackingNumber(orderId, "12345", unknownEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("운송장 등록 실패 - 존재하지 않는 주문")
    void registerTrackingNumber_Fail_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        String email = "seller@test.com";
        User user = User.createUser(email, "pw", "seller", "010-2222-2222");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.registerTrackingNumber(orderId, "12345", email)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 실패 - 존재하지 않는 관리자")
    void updateOrderStatus_Fail_AdminNotFound() {
        UUID orderId = UUID.randomUUID();
        String unknownEmail = "unknown@test.com";

        given(adminRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(orderId, OrderStatus.INSPECTION_PASSED, null, unknownEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADMIN_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 실패 - 존재하지 않는 주문")
    void updateOrderStatus_Fail_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        String email = "admin@test.com";
        Admin admin = org.mockito.Mockito.mock(Admin.class);
        given(admin.getAdminRole()).willReturn(AdminRole.ROLE_MASTER);

        given(adminRepository.findByEmail(email)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(orderId, OrderStatus.INSPECTION_PASSED, null, email)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 실패 - 운송장 번호가 빈 문자열(Blank)")
    void updateOrderStatus_Fail_EmptyTrackingNumber() {
        UUID orderId = UUID.randomUUID();
        String email = "admin@test.com";
        Admin admin = org.mockito.Mockito.mock(Admin.class);
        given(admin.getAdminRole()).willReturn(AdminRole.ROLE_MASTER);

        Order order = Order.builder().price(BigDecimal.valueOf(300000)).build();
        ReflectionTestUtils.setField(order, "status", OrderStatus.INSPECTION_PASSED);

        given(adminRepository.findByEmail(email)).willReturn(Optional.of(admin));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED_TO_BUYER, "", email)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRACKING_NUMBER_REQUIRED);
    }

    @Test
    @DisplayName("구매 확정 실패 - 존재하지 않는 사용자")
    void confirmOrder_Fail_UserNotFound() {
        UUID orderId = UUID.randomUUID();
        String unknownEmail = "unknown@test.com";

        given(userRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.confirmOrder(orderId, unknownEmail)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("구매 확정 실패 - 존재하지 않는 주문")
    void confirmOrder_Fail_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        String email = "buyer@test.com";
        User user = User.createUser(email, "pw", "buyer", "010-1111-1111");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.confirmOrder(orderId, email)
        ).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }
}