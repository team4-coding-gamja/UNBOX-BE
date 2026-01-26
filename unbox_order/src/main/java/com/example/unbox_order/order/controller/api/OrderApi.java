package com.example.unbox_order.order.controller.api;

import com.example.unbox_order.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_order.order.dto.request.OrderTrackingRequestDto;
import com.example.unbox_order.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_order.order.dto.response.OrderResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "[사용자] 주문 관리", description = "주문 관리 API")
public interface OrderApi {

    @Operation(summary = "주문 생성", description = "구매자가 상품을 주문합니다.")
    CustomApiResponse<UUID> createOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = OrderCreateRequestDto.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "주문 생성 예시",
                                            value = """
                                            {
                                              "sellingBidId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                              "receiverName": "홍길동",
                                              "receiverPhone": "010-1234-5678",
                                              "receiverAddress": "서울시 강남구 테헤란로 123",
                                              "receiverZipCode": "12345"
                                            }
                                            """
                                    )
                            }
                    )
            )
            @Valid @RequestBody OrderCreateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );



    @Operation(summary = "내 주문 목록 조회", description = "구매자가 자신의 주문 내역을 페이징 조회합니다.")
    CustomApiResponse<Page<OrderResponseDto>> getMyOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(summary = "주문 상세 조회", description = "주문의 상세 정보(배송지, 옵션 등)를 조회합니다.")
    CustomApiResponse<OrderDetailResponseDto> getOrderDetail(
            @PathVariable UUID orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "주문 취소", description = "주문을 취소합니다.")
    CustomApiResponse<OrderDetailResponseDto> cancelOrder(
            @PathVariable UUID orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "운송장 등록 (판매자용)", description = "판매자가 운송장 번호를 등록하고 배송을 시작합니다.")
    CustomApiResponse<OrderDetailResponseDto> registerTracking(
            @PathVariable UUID orderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = OrderTrackingRequestDto.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "판매자 운송장 등록",
                                            value = """
                                            {
                                              "trackingNumber": "S-TRACK-123456"
                                            }
                                            """
                                    )
                            }
                    )
            )
            @Valid @RequestBody OrderTrackingRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "구매 확정 (구매자용)", description = "배송 완료된 주문을 구매 확정합니다.")
    CustomApiResponse<OrderDetailResponseDto> confirmOrder(
            @PathVariable UUID orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}