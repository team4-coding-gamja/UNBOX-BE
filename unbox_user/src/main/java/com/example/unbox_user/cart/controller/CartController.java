package com.example.unbox_user.cart.controller;

import com.example.unbox_user.cart.controller.api.CartApi;
import com.example.unbox_user.cart.dto.request.CartCreateRequestDto;
import com.example.unbox_user.cart.dto.response.CartCreateResponseDto;
import com.example.unbox_user.cart.dto.response.CartListResponseDto;
import com.example.unbox_user.cart.service.CartService;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController implements CartApi {

    private final CartService cartService;

    @PostMapping
    public CustomApiResponse<CartCreateResponseDto> createCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CartCreateRequestDto requestDto
    ) {
        CartCreateResponseDto result = cartService.createCart(userDetails.getUserId(), requestDto);
        return CustomApiResponse.success(result);
    }

    @GetMapping
    public CustomApiResponse<List<CartListResponseDto>> getMyCarts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CartListResponseDto> result = cartService.getMyCarts(userDetails.getUserId());
        return CustomApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public CustomApiResponse<Void> deleteCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        cartService.deleteCart(userDetails.getUserId(), id);
        return CustomApiResponse.success(null);
    }

    @DeleteMapping
    public CustomApiResponse<Void> deleteAllCarts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.deleteAllCarts(userDetails.getUserId());
        return CustomApiResponse.success(null);
    }
}
