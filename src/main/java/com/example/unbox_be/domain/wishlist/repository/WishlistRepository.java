package com.example.unbox_be.domain.wishlist.repository;

import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.wishlist.entity.Wishlist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    // 특정 유저가 특정 상품 옵션을 이미 찜했는지 확인 (중복 방지용)
    boolean existsByUserAndProductOption(User user, ProductOption productOption);

    // 내 위시리스트 목록 조회
    @EntityGraph(attributePaths = {"productOption", "productOption.product"})
    Slice<Wishlist> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}