package com.example.unbox_be.domain.reviews.repository;

import com.example.unbox_be.domain.reviews.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByIdAndDeletedAtIsNull(UUID reviewId);

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);
}