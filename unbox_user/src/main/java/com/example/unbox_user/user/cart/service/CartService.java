package com.example.unbox_user.user.cart.service;

import com.example.unbox_user.user.cart.dto.request.CartCreateRequestDto;
import com.example.unbox_user.user.cart.dto.response.CartCreateResponseDto;
import com.example.unbox_user.user.cart.dto.response.CartListResponseDto;

import java.util.List;
import java.util.UUID;

public interface CartService {
    // 장바구니 담기
    CartCreateResponseDto createCart(Long userId, CartCreateRequestDto requestDto);

    // 내 장바구니 목록 조회
    List<CartListResponseDto> getMyCarts(Long userId);

    // 장바구니 항목 삭제 (단건)
    void deleteCart(Long userId, UUID cartId);

    // 장바구니 비우기 (전체 삭제)
    void deleteAllCarts(Long userId);
}
