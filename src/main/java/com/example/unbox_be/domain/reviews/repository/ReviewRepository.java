package com.example.unbox_be.domain.reviews.repository;

import com.example.unbox_be.domain.reviews.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


// 리뷰 도메인의 Repository 인터페이스
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    // save, findByIdAndDeletedAtIsNull, delete 기능 자동생성

    // 상품별 리뷰 리스트 조회 (삭제되지 않은 것만)
    Page<Review> findAllByProductIdAndDeletedAtIsNull(UUID productId, Pageable pageable);

    // 해당 주문에 대한 리뷰가 이미 있는지 확인
    boolean existsByOrderId(UUID orderId);

    Optional<Review> findByIdAndDeletedAtIsNull(UUID reviewId);
}