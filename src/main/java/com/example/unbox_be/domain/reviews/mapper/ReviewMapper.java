package com.example.unbox_be.domain.reviews.mapper;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.client.order.dto.OrderForReviewInfoResponse;
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

    /* =====================
     * 리뷰 상세 응답
     * ===================== */
    @Mapping(target = "order", source = "orderInfo")
    ReviewDetailResponseDto toReviewDetailResponseDto(Review review);

    /* =====================
     * Order -> OrderInfo
     * ===================== */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "buyerId", source = "buyerId")
    @Mapping(target = "status", source = "status")
    ReviewDetailResponseDto.OrderInfo toOrderInfo(OrderForReviewInfoResponse orderInfo);

    /* =====================
     * User -> UserInfo
     * ===================== */
    ReviewDetailResponseDto.UserInfo toUserInfo(User user);

    /* =====================
     * Order (Snapshot) -> ProductOptionInfo
     * ===================== */
    @Mapping(target = "id", source = "productOptionId")
    @Mapping(target = "option", source = "optionName")
    @Mapping(target = "product", source = ".") // calls toProductInfo(Order)
    ReviewDetailResponseDto.ProductOptionInfo toProductOptionInfo(Order order);

    /* =====================
     * Order (Snapshot) -> ProductInfo
     * ===================== */
    @Mapping(target = "id", source = "productId")
    @Mapping(target = "name", source = "productName")
    @Mapping(target = "imageUrl", source = "imageUrl")
    ReviewDetailResponseDto.ProductInfo toProductInfo(Order order);
}