package com.example.unbox_be.user.cart.service;

import com.example.unbox_be.user.cart.dto.request.CartCreateRequestDto;
import com.example.unbox_be.user.cart.dto.response.CartCreateResponseDto;
import com.example.unbox_be.user.cart.dto.response.CartListResponseDto;
import com.example.unbox_be.user.cart.entity.Cart;
import com.example.unbox_be.user.cart.repository.CartRepository;
import com.example.unbox_be.trade.entity.SellingBid;
import com.example.unbox_be.trade.entity.SellingStatus;
import com.example.unbox_be.trade.service.SellingBidService;
import com.example.unbox_be.user.user.entity.User;
import com.example.unbox_be.user.user.service.UserService;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
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
    public CartCreateResponseDto createCart(Long userId, CartCreateRequestDto requestDto) {
        User user = userService.findUser(userId);
        SellingBid sellingBid = sellingBidService.findSellingBidById(requestDto.getSellingBidId());

        // 1. 상태 검증: 판매중(LIVE)인 상품만 담을 수 있음
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.BID_NOT_AVAILABLE_FOR_CART);
        }

        // 2. 본인 상품 검증: 본인이 올린 상품은 담을 수 없음
        if (Objects.equals(sellingBid.getSellerId(), userId)) {
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

        Cart save = cartRepository.save(cart);
        return CartCreateResponseDto.builder()
                .sellingBidId(save.getSellingBid().getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartListResponseDto> getMyCarts(Long userId) {
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

    private CartListResponseDto toResponseDto(Cart cart) {
        SellingBid bid = cart.getSellingBid();

        return CartListResponseDto.builder()
                .cartId(cart.getId())
                .createdAt(cart.getCreatedAt())

                .sellingBidId(bid.getId())
                .price(bid.getPrice())
                .sellingStatus(bid.getStatus())

                .productOptionId(bid.getProductOptionId())
                .productOptionName(cart.getProductOptionName())

                .productName(cart.getProductName())
                .modelNumber(cart.getModelName())
                .productImageUrl(cart.getProductImageUrl())
                .build();
    }
}
