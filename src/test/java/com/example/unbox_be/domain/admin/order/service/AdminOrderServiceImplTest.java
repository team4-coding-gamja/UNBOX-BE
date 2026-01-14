//package com.example.unbox_be.domain.admin.order.service;
//
//import com.example.unbox_be.domain.order.dto.OrderSearchCondition;
//import com.example.unbox_be.domain.order.repository.AdminOrderRepository;
//import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
//import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
//import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
//import com.example.unbox_be.domain.order.entity.Order;
//import com.example.unbox_be.domain.order.entity.OrderStatus;
//import com.example.unbox_be.domain.order.mapper.OrderMapper;
//import com.example.unbox_be.domain.order.service.AdminOrderServiceImpl;
//import com.example.unbox_be.global.error.exception.CustomException;
//import com.example.unbox_be.global.error.exception.ErrorCode;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AdminOrderServiceImplTest {
//
//    @Mock
//    private AdminOrderRepository adminOrderRepository;
//
//    @Mock
//    private OrderMapper orderMapper;
//
//    @InjectMocks
//    private AdminOrderServiceImpl adminOrderService;
//
//    @Nested
//    @DisplayName("getAdminOrders")
//    class GetAdminOrders {
//
//        @Test
//        @DisplayName("성공: 레포지토리 조회 결과를 DTO로 매핑하여 Page로 반환한다")
//        void 주문목록을조회한다_정상요청이면_DTO페이지를반환한다() {
//            // given
//            OrderSearchCondition condition = new OrderSearchCondition();
//            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
//
//            Order order1 = mock(Order.class);
//            Order order2 = mock(Order.class);
//
//            Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), pageable, 2);
//
//            OrderResponseDto dto1 = mock(OrderResponseDto.class);
//            OrderResponseDto dto2 = mock(OrderResponseDto.class);
//
//            when(adminOrderRepository.findAdminOrders(condition, pageable)).thenReturn(orderPage);
//            when(orderMapper.toResponseDto(order1)).thenReturn(dto1);
//            when(orderMapper.toResponseDto(order2)).thenReturn(dto2);
//
//            // when
//            Page<OrderResponseDto> result = adminOrderService.getAdminOrders(condition, pageable);
//
//            // then
//            assertThat(result.getContent()).containsExactly(dto1, dto2);
//            assertThat(result.getTotalElements()).isEqualTo(2);
//
//            verify(adminOrderRepository).findAdminOrders(condition, pageable);
//            verify(orderMapper).toResponseDto(order1);
//            verify(orderMapper).toResponseDto(order2);
//            verifyNoMoreInteractions(adminOrderRepository, orderMapper);
//        }
//    }
//
//    @Nested
//    @DisplayName("getAdminOrderDetail")
//    class GetAdminOrderDetail {
//
//        @Test
//        @DisplayName("성공: findWithDetailsById로 조회 후 상세 DTO로 매핑한다")
//        void 주문상세를조회한다_주문이존재하면_상세DTO를반환한다() {
//            // given
//            UUID orderId = UUID.randomUUID();
//
//            Order order = mock(Order.class);
//            OrderDetailResponseDto detailDto = mock(OrderDetailResponseDto.class);
//
//            when(adminOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
//            when(orderMapper.toDetailResponseDto(order)).thenReturn(detailDto);
//
//            // when
//            OrderDetailResponseDto result = adminOrderService.getAdminOrderDetail(orderId);
//
//            // then
//            assertThat(result).isSameAs(detailDto);
//
//            verify(adminOrderRepository).findWithDetailsById(orderId);
//            verify(orderMapper).toDetailResponseDto(order);
//            verifyNoMoreInteractions(adminOrderRepository, orderMapper);
//        }
//
//        @Test
//        @DisplayName("실패: 주문이 없으면 ORDER_NOT_FOUND 예외를 던진다")
//        void 주문상세를조회한다_주문이없으면_ORDER_NOT_FOUND_예외가발생한다() {
//            // given
//            UUID orderId = UUID.randomUUID();
//            when(adminOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() -> adminOrderService.getAdminOrderDetail(orderId))
//                    .isInstanceOf(CustomException.class);
//
//            verify(adminOrderRepository).findWithDetailsById(orderId);
//            verifyNoInteractions(orderMapper);
//        }
//    }
//
//    @Nested
//    @DisplayName("updateAdminStatus")
//    class UpdateAdminStatus {
//
//        @Test
//        @DisplayName("성공: 주문 조회 후 Order.updateAdminStatus를 호출하고 상세 DTO로 반환한다")
//        void 주문상태를변경한다_정상요청이면_상태변경후상세DTO를반환한다() {
//            // given
//            UUID orderId = UUID.randomUUID();
//            Order order = mock(Order.class);
//
//            OrderStatusUpdateRequestDto requestDto =
//                    new OrderStatusUpdateRequestDto(OrderStatus.SHIPPED_TO_BUYER, "TRACK-123");
//
//            OrderDetailResponseDto detailDto = mock(OrderDetailResponseDto.class);
//
//            when(adminOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
//            when(orderMapper.toDetailResponseDto(order)).thenReturn(detailDto);
//
//            // when
//            OrderDetailResponseDto result = adminOrderService.updateAdminStatus(orderId, requestDto);
//
//            // then
//            assertThat(result).isSameAs(detailDto);
//
//            verify(adminOrderRepository).findWithDetailsById(orderId);
//
//            // Order.updateAdminStatus가 정확한 값으로 호출됐는지 검증 (실무에서 자주 함)
//            verify(order).updateAdminStatus(OrderStatus.SHIPPED_TO_BUYER, "TRACK-123");
//
//            verify(orderMapper).toDetailResponseDto(order);
//            verifyNoMoreInteractions(adminOrderRepository, orderMapper, order);
//        }
//
//        @Test
//        @DisplayName("실패: 주문이 없으면 ORDER_NOT_FOUND 예외를 던진다")
//        void 주문상태를변경한다_주문이없으면_ORDER_NOT_FOUND_예외가발생한다() {
//            // given
//            UUID orderId = UUID.randomUUID();
//            OrderStatusUpdateRequestDto requestDto =
//                    new OrderStatusUpdateRequestDto(OrderStatus.ARRIVED_AT_CENTER, null);
//
//            when(adminOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() -> adminOrderService.updateAdminStatus(orderId, requestDto))
//                    .isInstanceOf(CustomException.class);
//
//            verify(adminOrderRepository).findWithDetailsById(orderId);
//            verifyNoInteractions(orderMapper);
//        }
//
//        @Test
//        @DisplayName("실패: 도메인(Order.updateAdminStatus)에서 예외가 터지면 그대로 전파되고 매핑은 호출되지 않는다")
//        void 주문상태를변경한다_도메인검증에실패하면_예외가그대로전파된다() {
//            // given
//            UUID orderId = UUID.randomUUID();
//            Order order = mock(Order.class);
//
//            OrderStatusUpdateRequestDto requestDto =
//                    new OrderStatusUpdateRequestDto(OrderStatus.SHIPPED_TO_BUYER, ""); // 빈 운송장 등
//
//            when(adminOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
//
//            // 도메인에서 터지는 예외를 시뮬레이션
//            doThrow(new CustomException(ErrorCode.TRACKING_NUMBER_REQUIRED))
//                    .when(order).updateAdminStatus(any(OrderStatus.class), anyString());
//
//            // when & then
//            assertThatThrownBy(() -> adminOrderService.updateAdminStatus(orderId, requestDto))
//                    .isInstanceOf(CustomException.class);
//
//            verify(adminOrderRepository).findWithDetailsById(orderId);
//            verify(order).updateAdminStatus(requestDto.getStatus(), requestDto.getTrackingNumber());
//
//            // 실패했으니 mapper는 호출되면 안 됨
//            verifyNoInteractions(orderMapper);
//        }
//
//        @Test
//        @DisplayName("상태 값 전달 검증: 캡처를 통해 updateAdminStatus에 넘어간 값을 확인한다")
//        void 주문상태를변경한다_요청값이_Order엔티티로정확히전달된다() {
//            // given
//            UUID orderId = UUID.randomUUID();
//            Order order = mock(Order.class);
//
//            OrderStatusUpdateRequestDto requestDto =
//                    new OrderStatusUpdateRequestDto(OrderStatus.DELIVERED, null);
//
//            when(adminOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
//            when(orderMapper.toDetailResponseDto(order)).thenReturn(mock(OrderDetailResponseDto.class));
//
//            ArgumentCaptor<OrderStatus> statusCaptor = ArgumentCaptor.forClass(OrderStatus.class);
//            ArgumentCaptor<String> trackingCaptor = ArgumentCaptor.forClass(String.class);
//
//            // when
//            adminOrderService.updateAdminStatus(orderId, requestDto);
//
//            // then
//            verify(order).updateAdminStatus(statusCaptor.capture(), trackingCaptor.capture());
//
//            assertThat(statusCaptor.getValue()).isEqualTo(OrderStatus.DELIVERED);
//            assertThat(trackingCaptor.getValue()).isNull();
//        }
//    }
//}