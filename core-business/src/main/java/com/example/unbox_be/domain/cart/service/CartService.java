package com.example.unbox_be.domain.cart.service;

import com.example.unbox_be.domain.cart.dto.request.CartRequestDto;
import com.example.unbox_be.domain.cart.dto.response.CartResponseDto;

import java.util.List;

public interface CartService {
    // 장바구니 담기
    void addCart(Long userId, CartRequestDto requestDto);

    // 내 장바구니 목록 조회
    List<CartResponseDto> getMyCarts(Long userId);

    // 장바구니 항목 삭제 (단건)
    void deleteCart(Long userId, Long cartId);

    // 장바구니 비우기 (전체 삭제)
    void deleteAllCarts(Long userId);
}
