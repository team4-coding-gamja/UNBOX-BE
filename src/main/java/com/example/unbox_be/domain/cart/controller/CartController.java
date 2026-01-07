package com.example.unbox_be.domain.cart.controller;

import com.example.unbox_be.domain.cart.controller.api.CartApi;
import com.example.unbox_be.domain.cart.dto.request.CartRequestDto;
import com.example.unbox_be.domain.cart.dto.response.CartResponseDto;
import com.example.unbox_be.domain.cart.service.CartService;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CartController implements CartApi {

    private final CartService cartService;

    @Override
    public CustomApiResponse<Void> addCart(CustomUserDetails userDetails, CartRequestDto requestDto) {
        cartService.addCart(userDetails.getUserId(), requestDto);
        return CustomApiResponse.success(null);
    }

    @Override
    public CustomApiResponse<List<CartResponseDto>> getMyCarts(CustomUserDetails userDetails) {
        List<CartResponseDto> result = cartService.getMyCarts(userDetails.getUserId());
        return CustomApiResponse.success(result);
    }

    @Override
    public CustomApiResponse<Void> deleteCart(CustomUserDetails userDetails, Long cartId) {
        cartService.deleteCart(userDetails.getUserId(), cartId);
        return CustomApiResponse.success(null);
    }

    @Override
    public CustomApiResponse<Void> deleteAllCarts(CustomUserDetails userDetails) {
        cartService.deleteAllCarts(userDetails.getUserId());
        return CustomApiResponse.success(null);
    }
}
