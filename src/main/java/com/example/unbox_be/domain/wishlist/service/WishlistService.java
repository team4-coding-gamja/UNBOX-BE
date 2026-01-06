package com.example.unbox_be.domain.wishlist.service;

import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.domain.wishlist.dto.response.WishlistResponseDTO;
import com.example.unbox_be.domain.wishlist.entity.Wishlist;
import com.example.unbox_be.domain.wishlist.repository.WishlistRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductOptionRepository productOptionRepository;
    private final UserRepository userRepository; // 유저 조회를 위해 추가

    // 1. 위시리스트 추가
    @Transactional
    public void addWishlist(String email, UUID optionId) {
        // 이메일로 영속 상태의 유저 엔티티 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ProductOption option = productOptionRepository.findByIdAndDeletedAtIsNull(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        // 중복 체크 (영속화된 user 객체 사용)
        if (wishlistRepository.existsByUserAndProductOption(user, option)) {
            throw new CustomException(ErrorCode.WISHLIST_ALREADY_EXISTS);
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .productOption(option)
                .build();

        wishlistRepository.save(wishlist);
    }

    // 2. 내 위시리스트 목록 조회
    @Transactional(readOnly = true)
    public List<WishlistResponseDTO> getMyWishlist(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 해당 유저의 위시리스트 조회
        Slice<Wishlist> wishlistSlice = wishlistRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return wishlistSlice.getContent().stream()
                .map(WishlistResponseDTO::from)
                .toList();
    }

    // 3. 위시리스트 삭제
    @Transactional
    public void removeWishlist(String email, UUID wishlistId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Wishlist wishlist = wishlistRepository.findByIdAndDeletedAtIsNull(wishlistId)
                .orElseThrow(() -> new CustomException(ErrorCode.WISHLIST_NOT_FOUND));

        // 보안 검증: 삭제하려는 항목의 주인이 현재 로그인한 유저인지 확인
        if (!wishlist.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // Soft Delete
        wishlist.softDelete(email);
    }
}