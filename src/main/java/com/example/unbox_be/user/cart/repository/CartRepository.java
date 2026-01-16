package com.example.unbox_be.user.cart.repository;

import com.example.unbox_be.user.cart.entity.Cart;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import com.example.unbox_be.user.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // 중복 체크 (사용자 + 판매입찰)
    boolean existsByUserAndSellingBid(User user, SellingBid sellingBid);

    // 내 장바구니 목록 조회 (상품 정보 Fetch Join)
    @EntityGraph(attributePaths = {"sellingBid", "sellingBid.productOption", "sellingBid.productOption.product", "sellingBid.productOption.product.brand"})
    List<Cart> findAllByUserOrderByCreatedAtDesc(User user);

    // 단건 조회
    Optional<Cart> findByIdAndDeletedAtIsNull(Long id);

    // 삭제할 목록 조회
    List<Cart> findAllByUser(User user);
}
