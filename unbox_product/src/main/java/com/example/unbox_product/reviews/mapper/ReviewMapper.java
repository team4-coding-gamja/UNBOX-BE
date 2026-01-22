package com.example.unbox_product.reviews.mapper;

import com.example.unbox_product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_product.reviews.entity.Review;
import com.example.unbox_product.reviews.entity.ReviewProductSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReviewMapper {

        @Mapping(target = "reviewId", source = "id")
        @Mapping(target = "content", source = "content")
        @Mapping(target = "reviewImageUrl", source = "imageUrl")
        @Mapping(target = "rating", source = "rating")
        ReviewCreateResponseDto toReviewCreateResponseDto(Review review);

        @Mapping(target = "reviewId", source = "id")
        @Mapping(target = "content", source = "content")
        @Mapping(target = "reviewImageUrl", source = "imageUrl")
        @Mapping(target = "rating", source = "rating")
        ReviewUpdateResponseDto toReviewUpdateResponseDto(Review review);

        @Mapping(target = "reviewId", source = "id")
        @Mapping(target = "content", source = "content")
        @Mapping(target = "rating", source = "rating")
        @Mapping(target = "reviewImageUrl", source = "imageUrl")
        @Mapping(target = "createdAt", source = "createdAt")
        @Mapping(target = "buyerNickname", source = "buyerNickname")
        @Mapping(target = "productName", source = "productSnapshot.productName")
        @Mapping(target = "productOptionName", source = "productSnapshot.productOptionName")
        ReviewListResponseDto toReviewListResponseDto(Review review);

        @Mapping(target = "reviewId", source = "id")
        @Mapping(target = "content", source = "content")
        @Mapping(target = "reviewImageUrl", source = "imageUrl")
        @Mapping(target = "rating", source = "rating")
        @Mapping(target = "createdAt", source = "createdAt")
        @Mapping(target = "order", expression = "java(buildOrderInfo(review))")
        ReviewDetailResponseDto toReviewDetailResponseDto(Review review);

        default ReviewDetailResponseDto.OrderInfo buildOrderInfo(Review review) {
                ReviewProductSnapshot s = review.getProductSnapshot();
                if (s == null)
                        return null;

                return ReviewDetailResponseDto.OrderInfo.builder()
                                .orderId(review.getOrderId())
                                .buyerNickname(review.getBuyerNickname())
                                .productOption(
                                                ReviewDetailResponseDto.ProductOptionInfo.builder()
                                                                .productOptionId(s.getProductOptionId())
                                                                .productOptionName(s.getProductOptionName())
                                                                .product(
                                                                                ReviewDetailResponseDto.ProductInfo
                                                                                                .builder()
                                                                                                .productId(s.getProductId())
                                                                                                .productName(s.getProductName())
                                                                                                .productImageUrl(s
                                                                                                                .getProductImageUrl())
                                                                                                .build())
                                                                .build())
                                .build();
        }
}
