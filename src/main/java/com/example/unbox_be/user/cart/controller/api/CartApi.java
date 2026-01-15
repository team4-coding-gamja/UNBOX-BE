package com.example.unbox_be.user.cart.controller.api;

import com.example.unbox_be.user.cart.dto.request.CartCreateRequestDto;
import com.example.unbox_be.user.cart.dto.response.CartListResponseDto;
import com.example.unbox_be.common.response.CustomApiResponse;
import com.example.unbox_be.common.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "장바구니 관리", description = "장바구니 관련 API")
@RequestMapping("/api/carts")
public interface CartApi {

    @Operation(summary = "장바구니 담기", description = "판매 중인 상품을 장바구니에 담습니다.")
    CustomApiResponse<Void> addCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CartCreateRequestDto requestDto
    );

    @Operation(summary = "내 장바구니 목록 조회", description = "장바구니 목록을 조회합니다.")
    CustomApiResponse<List<CartListResponseDto>> getMyCarts(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "장바구니 항목 삭제 (단건)", description = "장바구니의 특정 항목을 비웁니다. (구매나 삭제 등으로)")
    CustomApiResponse<Void> deleteCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartId
    );

    @Operation(summary = "장바구니 비우기 (전체 삭제)", description = "장바구니의 모든 항목을 비웁니다.")
    CustomApiResponse<Void> deleteAllCarts(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
