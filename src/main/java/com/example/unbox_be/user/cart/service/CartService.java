package com.example.unbox_be.user.cart.service;

import com.example.unbox_be.user.cart.dto.request.CartCreateRequestDto;
import com.example.unbox_be.user.cart.dto.response.CartCreateResponseDto;
import com.example.unbox_be.user.cart.dto.response.CartListResponseDto;

import java.util.List;

public interface CartService {
    // 장바구니 담기
    CartCreateResponseDto createCart(Long userId, CartCreateRequestDto requestDto);

    // 내 장바구니 목록 조회
    List<CartListResponseDto> getMyCarts(Long userId);

    // 장바구니 항목 삭제 (단건)
    void deleteCart(Long userId, Long cartId);

    // 장바구니 비우기 (전체 삭제)
    void deleteAllCarts(Long userId);
}
