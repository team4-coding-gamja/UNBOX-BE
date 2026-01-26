package com.example.unbox_order.order.controller;

import com.example.unbox_order.order.controller.api.OrderApi;
import com.example.unbox_order.order.dto.request.OrderCreateRequestDto;

import com.example.unbox_order.order.dto.request.OrderTrackingRequestDto;
import com.example.unbox_order.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_order.order.dto.response.OrderResponseDto;
import com.example.unbox_order.order.application.service.OrderService;
import com.example.unbox_common.pagination.PageSizeLimiter;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @PostMapping
    public CustomApiResponse<UUID> createOrder(
            @RequestBody OrderCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID orderId = orderService.createOrder(requestDto, userDetails.getUserId());
        return CustomApiResponse.success(orderId);
    }

    @GetMapping
    public CustomApiResponse<Page<OrderResponseDto>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<OrderResponseDto> response = orderService.getMyOrders(userDetails.getUserId(), limited);
        return CustomApiResponse.success(response);
    }

    @GetMapping("/{orderId}")
    public CustomApiResponse<OrderDetailResponseDto> getOrderDetail(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 상세 조회는 구매자/판매자(User) 또는 관리자(Admin) 모두 가능
        // 우선 User ID를 넘기되, 관리자인 경우 서비스에서 별도 처리가 필요할 수 있음 (현재는 User 로직 위주)
        // 만약 관리자도 조회해야 한다면 서비스의 getOrderDetail 인자를 확장하거나 분기 처리가 필요함.
        // 일단 기존 로직 유지를 위해 getUserId() 사용 (User 기준)
        Long userId = userDetails.getUserId();
        // Tip: 관리자 조회 기능이 필요하면 여기서 userDetails.getAdminId() 체크 로직 추가 권장

        OrderDetailResponseDto response = orderService.getOrderDetail(orderId, userId);
        return CustomApiResponse.success(response);
    }

    @PatchMapping("/{orderId}/cancel")
    public CustomApiResponse<OrderDetailResponseDto> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderDetailResponseDto response = orderService.cancelOrder(orderId, userDetails.getUserId());
        return CustomApiResponse.success(response);
    }

    @PatchMapping("/{orderId}/tracking")
    public CustomApiResponse<OrderDetailResponseDto> registerTracking(
            @PathVariable UUID orderId,
            OrderTrackingRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderDetailResponseDto response = orderService.registerTracking(
                orderId,
                requestDto.getTrackingNumber(),
                userDetails.getUserId()
        );
        return CustomApiResponse.success(response);
    }

    @PatchMapping("/{orderId}/confirm")
    public CustomApiResponse<OrderDetailResponseDto> confirmOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderDetailResponseDto response = orderService.confirmOrder(orderId, userDetails.getUserId());
        return CustomApiResponse.success(response);
    }
}