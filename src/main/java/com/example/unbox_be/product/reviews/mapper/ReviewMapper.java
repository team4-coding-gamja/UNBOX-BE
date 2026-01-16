package com.example.unbox_be.product.reviews.mapper;

import com.example.unbox_be.order.entity.Order;
import com.example.unbox_be.product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.product.reviews.entity.Review;
import com.example.unbox_be.user.user.entity.User;
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

    @Mapping(target = "buyerNickname", source = "order.buyer.nickname")
    @Mapping(target = "productOptionName", source = "order.productOptionName")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "reviewId", source = "id")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "reviewImageUrl", source = "imageUrl")
    @Mapping(target = "rating", source = "rating")
    ReviewListResponseDto toReviewListResponseDto(Review review);

    @Mapping(target = "reviewId", source = "id")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "reviewImageUrl", source = "imageUrl")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "order", source = "order")
    ReviewDetailResponseDto toReviewDetailResponseDto(Review review);



    @Mapping(target = "buyer", source = "buyer") // User -> UserInfo 자동 호출
    @Mapping(target = "productOption", source = ".") // Order -> ProductOptionInfo 자동 호출
    ReviewDetailResponseDto.OrderInfo toOrderInfo(Order order);

    ReviewDetailResponseDto.UserInfo toUserInfo(User user);

    @Mapping(target = "id", source = "productOptionId")
    @Mapping(target = "productOptionName", source = "productOptionName")
    @Mapping(target = "product", source = ".") // Order -> ProductInfo 자동 호출
    ReviewDetailResponseDto.ProductOptionInfo toProductOptionInfo(Order order);

    @Mapping(target = "id", source = "productId")
    @Mapping(target = "name", source = "productName")
    @Mapping(target = "productImageUrl", source = "productImageUrl")
    ReviewDetailResponseDto.ProductInfo toProductInfo(Order order);
}