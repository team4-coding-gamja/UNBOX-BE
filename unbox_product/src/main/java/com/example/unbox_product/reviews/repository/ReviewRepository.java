package com.example.unbox_product.reviews.repository;

import com.example.unbox_product.reviews.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByIdAndDeletedAtIsNull(UUID reviewId);

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    List<Review> findAllByProductSnapshotProductIdAndDeletedAtIsNull(UUID productId);

    Page<Review> findAllByProductSnapshotProductIdAndDeletedAtIsNull(UUID productId, Pageable pageable);

    Page<Review> findAllByBuyerIdAndDeletedAtIsNull(Long buyerId, Pageable pageable);
}
