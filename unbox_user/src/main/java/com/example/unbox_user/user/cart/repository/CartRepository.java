package com.example.unbox_user.user.cart.repository;

import com.example.unbox_user.user.cart.entity.Cart;
import com.example.unbox_user.user.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    // 중복 체크 (사용자 + 판매입찰)
    boolean existsByUserAndSellingBidId(User user, UUID sellingBidId);

    // 내 장바구니 목록 조회 (상품 정보 Fetch Join)
    List<Cart> findAllByUserOrderByCreatedAtDesc(User user);

    // 단건 조회
    Optional<Cart> findByIdAndDeletedAtIsNull(UUID id);

    // 삭제할 목록 조회
    List<Cart> findAllByUser(User user);
}
