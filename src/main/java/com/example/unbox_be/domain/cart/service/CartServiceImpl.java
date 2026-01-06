package com.example.unbox_be.domain.cart.service;

import com.example.unbox_be.domain.cart.dto.request.CartRequestDto;
import com.example.unbox_be.domain.cart.dto.response.CartResponseDto;
import com.example.unbox_be.domain.cart.entity.Cart;
import com.example.unbox_be.domain.cart.repository.CartRepository;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.service.UserService;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserService userService;
    private final SellingBidService sellingBidService;

    @Override
    @Transactional
    public void addCart(Long userId, CartRequestDto requestDto) {
        User user = userService.findUser(userId);
        SellingBid sellingBid = sellingBidService.findSellingBidById(requestDto.getSellingBidId());

        // 1. 상태 검증: 판매중(LIVE)인 상품만 담을 수 있음
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.BID_NOT_AVAILABLE_FOR_CART);
        }

        // 2. 본인 상품 검증: 본인이 올린 상품은 담을 수 없음
        if (Objects.equals(sellingBid.getUserId(), userId)) {
            throw new CustomException(ErrorCode.CANNOT_ADD_MY_OWN_BID);
        }

        // 3. 중복 검증
        if (cartRepository.existsByUserAndSellingBid(user, sellingBid)) {
            throw new CustomException(ErrorCode.CART_ALREADY_EXISTS);
        }

        Cart cart = Cart.builder()
                .user(user)
                .sellingBid(sellingBid)
                .build();

        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartResponseDto> getMyCarts(Long userId) {
        User user = userService.findUser(userId);
        List<Cart> carts = cartRepository.findAllByUserOrderByCreatedAtDesc(user);

        return carts.stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCart(Long userId, Long cartId) {
        Cart cart = cartRepository.findByIdAndDeletedAtIsNull(cartId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

       // 본인 확인
       if (!cart.getUser().getId().equals(userId)) {
           throw new CustomException(ErrorCode.NOT_CART_OWNER);
       }
       
       // -> Soft Delete 사용
       cart.softDelete(cart.getUser().getEmail());
    }

    @Override
    @Transactional
    public void deleteAllCarts(Long userId) {
        User user = userService.findUser(userId);
        List<Cart> carts = cartRepository.findAllByUser(user);
        
        // 전체 Soft Delete
        carts.forEach(cart -> cart.softDelete(user.getEmail()));
    }

    private CartResponseDto toResponseDto(Cart cart) {
        SellingBid bid = cart.getSellingBid();
        return CartResponseDto.builder()
                .cartId(cart.getId())
                .createdAt(cart.getCreatedAt())
                .sellingBid(CartResponseDto.SellingBidInfo.builder()
                        .id(bid.getId())
                        .price(bid.getPrice())
                        .status(bid.getStatus())
                        .size(bid.getProductOption().getOption()) // N+1 @EntityGraph로 해결
                        .product(CartResponseDto.ProductInfo.builder()
                                .id(bid.getProductOption().getProduct().getId())
                                .name(bid.getProductOption().getProduct().getName())
                                .brandName(bid.getProductOption().getProduct().getBrand().getName())
                                .imageUrl(bid.getProductOption().getProduct().getImageUrl())
                                .build())
                        .build())
                .build();
    }
}
