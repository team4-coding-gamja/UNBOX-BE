package com.example.unbox_be.domain.order.service;

import com.example.unbox_be.domain.order.dto.OrderCreateRequestDto;
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
}