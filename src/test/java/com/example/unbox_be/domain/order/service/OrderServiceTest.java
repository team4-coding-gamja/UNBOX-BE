package com.example.unbox_be.domain.order.service;

import com.example.unbox_be.domain.order.dto.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.dto.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.mapper.OrderMapper;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
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
    @Spy
    private OrderMapper orderMapper;

    @Test
    @DisplayName("주문 생성 성공 테스트")
    void createOrder_Success() {
        // Given
        Long sellerId = 2L;
        String buyerEmail = "buyer@test.com";
        UUID optionId = UUID.randomUUID();
        BigDecimal price = BigDecimal.valueOf(300000);

        OrderCreateRequestDto requestDto = new OrderCreateRequestDto(
                sellerId, optionId, price,
                "홍길동", "010-1234-5678", "서울", "12345"
        );

        // Mock 데이터 생성
        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");

        // 실제 매퍼가 돌 때 NullPointerException이 안 나도록 Brand, Product까지 꽉 채워서 생성
        Brand brand = new Brand("Nike");
        Product product = new Product(brand, "Air Force 1", "CW2288-111", Category.SHOES, "http://img.url");
        ProductOption option = new ProductOption(product, "270");

        // 가짜 행동 정의
        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(userRepository.findById(sellerId)).willReturn(Optional.of(seller));
        given(productOptionRepository.findById(optionId)).willReturn(Optional.of(option));

        Order savedOrder = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option) // 옵션(안에 상품, 브랜드 포함) 주입
                .price(price)
                .receiverName("홍길동")
                .build();
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // When
        OrderResponseDto result = orderService.createOrder(requestDto, buyerEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualTo(price);
        assertThat(result.getBrandName()).isEqualTo("Nike"); // 실제 매핑이 잘 됐는지 확인 가능
        verify(orderRepository).save(any(Order.class)); // save 호출 여부 검증
    }

    @Test
    @DisplayName("주문 실패 - 존재하지 않는 구매자")
    void createOrder_Fail_UserNotFound() {
        // Given
        Long sellerId = 2L;
        String unknownEmail = "unknown@test.com"; // DB에 없는 이메일
        UUID optionId = UUID.randomUUID();

        OrderCreateRequestDto requestDto = new OrderCreateRequestDto(
                sellerId, optionId, BigDecimal.valueOf(300000),
                "수령인", "010-0000-0000", "주소", "12345"
        );

        // 찾으면 비어있다(Empty)고 응답 설정
        given(userRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        // When & Then: 예외가 터지는지 확인
        // CustomException이 발생해야 하고, 에러 코드는 USER_NOT_FOUND여야 한다
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

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder()
                .sellerId(unknownSellerId)
                .productOptionId(optionId)
                .price(BigDecimal.valueOf(300000))
                .receiverName("수령인")
                .receiverPhone("010-0000-0000")
                .receiverAddress("주소")
                .receiverZipCode("12345")
                .build();

        // 구매자는 찾았는데...
        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111")));
        // 판매자가 없다고 설정 !
        given(userRepository.findById(unknownSellerId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                        orderService.createOrder(requestDto, buyerEmail)
                ).isInstanceOf(com.example.unbox_be.global.error.exception.CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.example.unbox_be.global.error.exception.ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 실패 - 존재하지 않는 상품 옵션")
    void createOrder_Fail_OptionNotFound() {
        // Given
        Long sellerId = 2L;
        String buyerEmail = "buyer@test.com";
        UUID unknownOptionId = UUID.randomUUID();

        OrderCreateRequestDto requestDto = OrderCreateRequestDto.builder()
                .sellerId(sellerId)
                .productOptionId(unknownOptionId)
                .price(BigDecimal.valueOf(300000))
                .build();

        // 구매자, 판매자 다 있는데...
        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111")));
        given(userRepository.findById(sellerId)).willReturn(Optional.of(User.createUser("seller", "pw", "seller", "010-2222-2222")));
        // 상품 옵션이 없다고 설정 !
        given(productOptionRepository.findById(unknownOptionId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                        orderService.createOrder(requestDto, buyerEmail)
                ).isInstanceOf(com.example.unbox_be.global.error.exception.CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.example.unbox_be.global.error.exception.ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("내 구매 내역 조회 성공 테스트")
    void getMyOrders_Success() {
        // Given
        String email = "buyer@test.com";
        User buyer = User.createUser(email, "pw", "buyer", "010-1111-1111");
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(buyer));

        // 페이징 결과 Mocking
        List<Order> orders = List.of(
                Order.builder().price(BigDecimal.valueOf(10000)).build(),
                Order.builder().price(BigDecimal.valueOf(20000)).build()
        );
        Page<Order> orderPage = new PageImpl<>(orders);

        given(orderRepository.findAllByBuyerId(eq(buyer.getId()), eq(pageable))).willReturn(orderPage);
        // 여기서는 Order 객체에 Brand/Product 정보가 없으므로
        // Spy지만 예외적으로 가짜 결과(Stub)를 반환하도록 설정해야 함 (안 그러면 NPE 터짐)
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

        // 유저 조회 시 Empty 반환 (유저 없음)
        given(userRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                        orderService.getMyOrders(unknownEmail, pageable)
                ).isInstanceOf(com.example.unbox_be.global.error.exception.CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.example.unbox_be.global.error.exception.ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 상세 조회 성공 - 구매자가 조회 시")
    void getOrderDetail_Success_Buyer() {
        // Given
        UUID orderId = UUID.randomUUID();
        String buyerEmail = "buyer@test.com";

        // 유저 생성 및 ID 주입 (Reflection 사용)
        User buyer = User.createUser(buyerEmail, "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);

        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        // 연관 객체 생성
        Brand brand = new Brand("Nike");
        Product product = new Product(brand, "Air Force 1", "CW", Category.SHOES, "url");
        ProductOption option = new ProductOption(product, "270");

        // 주문 생성 및 주입
        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .receiverName("홍길동")
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);

        // Mocking
        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When
        OrderDetailResponseDto result = orderService.getOrderDetail(orderId, buyerEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        // 매퍼가 정상적으로 호출되었는지 확인 (Spy 사용했으므로 실제 DTO 변환됨)
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(300000));
    }

    @Test
    @DisplayName("주문 상세 조회 실패 - 권한 없는 사용자(제3자)")
    void getOrderDetail_Fail_AccessDenied() {
        // Given
        UUID orderId = UUID.randomUUID();
        String hackerEmail = "hacker@test.com";

        User hacker = User.createUser(hackerEmail, "pw", "hacker", "010-9999-9999");
        ReflectionTestUtils.setField(hacker, "id", 3L); // 제3자

        User buyer = User.createUser("buyer@test.com", "pw", "buyer", "010-1111-1111");
        ReflectionTestUtils.setField(buyer, "id", 1L);
        User seller = User.createUser("seller@test.com", "pw", "seller", "010-2222-2222");
        ReflectionTestUtils.setField(seller, "id", 2L);

        // Order는 Buyer(1)와 Seller(2)의 거래
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

        // Buyer나 Seller가 null인 비정상 주문 객체 생성 (Builder 패턴 사용 시 필드 누락 가능)
        // 여기서는 @Builder가 있지만 null이 들어갔다고 가정하거나 Mock 객체 사용
        Order brokenOrder = Order.builder()
                .buyer(null) // 문제 발생 지점
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

        // Buyer는 있지만 Seller가 null인 객체 생성
        Order brokenOrder = Order.builder()
                .buyer(user)  // Buyer는 있음 (앞 조건 통과)
                .seller(null) // Seller가 없음 (뒷 조건에서 걸림)
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

        // 유저를 찾지 못함 -> 예외 발생 (람다식 실행)
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

        // 연관 객체 (Mapper 오류 방지용)
        Brand brand = new Brand("Nike");
        Product product = new Product(brand, "Air Force 1", "CW", Category.SHOES, "url");
        ProductOption option = new ProductOption(product, "270");

        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .productOption(option)
                .price(BigDecimal.valueOf(300000))
                .build();
        // 초기 상태는 PENDING_SHIPMENT
        // 명시적으로 확인
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_SHIPMENT);

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When
        orderService.cancelOrder(orderId, buyerEmail);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull(); // 취소 시간 기록 확인
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

        // 연관 객체
        Brand brand = new Brand("Nike");
        Product product = new Product(brand, "Air Force 1", "CW", Category.SHOES, "url");
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
        // 제3자(3L)가 취소 시도 -> Access Denied
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

        // 강제로 배송 상태 변경 (SHIPPED_TO_CENTER 등) - Setter가 없다면 메서드 활용
        // Order 엔티티에 updateStatus 메서드나 필드 접근 필요
        // 여기선 TestUtils로 상태 강제 주입
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_CENTER);

        given(userRepository.findByEmail(buyerEmail)).willReturn(Optional.of(buyer));
        given(orderRepository.findWithDetailsById(orderId)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, buyerEmail))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }
}