package com.example.unbox_be.domain.order.controller;

import com.example.unbox_be.domain.order.controller.api.OrderApi;
import com.example.unbox_be.domain.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.domain.order.dto.request.OrderTrackingRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.service.OrderService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    public ResponseEntity<ApiResponse<UUID>> createOrder(
            OrderCreateRequestDto requestDto,
            CustomUserDetails userDetails
    ) {
        // 컨트롤러는 오직 '요청 수신'과 '응답 반환'에만 집중합니다.
        UUID orderId = orderService.createOrder(requestDto, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orderId));
    }

    @Override
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getMyOrders(
            CustomUserDetails userDetails,
            Pageable pageable
    ) {
        // 페이지 사이즈 검증 로직은 Service 내부로 이동시켰습니다.
        Page<OrderResponseDto> response = orderService.getMyOrders(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> getOrderDetail(
            UUID orderId,
            CustomUserDetails userDetails
    ) {
        // 상세 조회는 구매자/판매자(User) 또는 관리자(Admin) 모두 가능
        // 우선 User ID를 넘기되, 관리자인 경우 서비스에서 별도 처리가 필요할 수 있음 (현재는 User 로직 위주)
        // 만약 관리자도 조회해야 한다면 서비스의 getOrderDetail 인자를 확장하거나 분기 처리가 필요함.
        // 일단 기존 로직 유지를 위해 getUserId() 사용 (User 기준)
        Long userId = userDetails.getUserId();
        // Tip: 관리자 조회 기능이 필요하면 여기서 userDetails.getAdminId() 체크 로직 추가 권장

        OrderDetailResponseDto response = orderService.getOrderDetail(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> cancelOrder(
            UUID orderId,
            CustomUserDetails userDetails
    ) {
        OrderDetailResponseDto response = orderService.cancelOrder(orderId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> registerTracking(
            UUID orderId,
            OrderTrackingRequestDto requestDto,
            CustomUserDetails userDetails
    ) {
        OrderDetailResponseDto response = orderService.registerTracking(
                orderId,
                requestDto.getTrackingNumber(),
                userDetails.getUserId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> updateOrderStatus(
            UUID orderId,
            OrderStatusUpdateRequestDto requestDto,
            CustomUserDetails userDetails
    ) {
        Long adminId = userDetails.getAdminId();

        OrderDetailResponseDto response = orderService.updateAdminStatus(
                orderId,
                requestDto.getStatus(),
                requestDto.getTrackingNumber(),
                adminId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> confirmOrder(
            UUID orderId,
            CustomUserDetails userDetails
    ) {
        OrderDetailResponseDto response = orderService.confirmOrder(orderId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}