package com.example.unbox_be.domain.reviews.mapper;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ReviewMapper {

    /* =====================
     * 리뷰 생성 응답
     * ===================== */
    ReviewCreateResponseDto toReviewCreateResponseDto(Review review);

    /* =====================
     * 리뷰 수정 응답
     * ===================== */
    ReviewUpdateResponseDto toReviewUpdateResponseDto(Review review);

    // ===== 리뷰 상세 =====
    @Mapping(target = "order", source = "order")
    @Mapping(target = "product", source = "product")
    @Mapping(target = "user", source = "buyer")
    ReviewDetailResponseDto toReviewDetailResponseDto(Review review);

    // ===== inner DTO 변환 =====
    @SuppressWarnings("unused")
    ReviewDetailResponseDto.OrderInfo toOrderInfo(Order order);

    @SuppressWarnings("unused")
    ReviewDetailResponseDto.ProductInfo toProductInfo(Product product);

    @SuppressWarnings("unused")
    ReviewDetailResponseDto.UserInfo toUserInfo(User user);
}