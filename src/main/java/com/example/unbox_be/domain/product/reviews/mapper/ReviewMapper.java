package com.example.unbox_be.domain.product.reviews.mapper;

import com.example.unbox_be.domain.order.order.entity.Order;
import com.example.unbox_be.domain.product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.domain.product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.domain.product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_be.domain.product.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.domain.product.reviews.entity.Review;
import com.example.unbox_be.domain.user.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReviewMapper {

    /*
     * =====================
     * 리뷰 생성 응답
     * =====================
     */
    ReviewCreateResponseDto toReviewCreateResponseDto(Review review);

    /*
     * =====================
     * 리뷰 수정 응답
     * =====================
     */
    ReviewUpdateResponseDto toReviewUpdateResponseDto(Review review);

    /*
     * =====================
     * 리뷰 상세 응답
     * Review.order(Order) -> ReviewDetailResponseDto.order(OrderInfo)
     * =====================
     */
    @Mapping(target = "order", source = "order") // Order -> OrderInfo 자동 호출
    ReviewDetailResponseDto toReviewDetailResponseDto(Review review);

    /*
     * =====================
     * 리뷰 리스트 응답
     * =====================
     */
    @Mapping(target = "buyerNickname", source = "order.buyer.nickname")
    @Mapping(target = "productOptionName", source = "order.productOptionName")
    @Mapping(target = "createdAt", source = "createdAt")
    ReviewListResponseDto toReviewListResponseDto(Review review);

    /*
     * =====================
     * Order -> OrderInfo
     * =====================
     */
    @Mapping(target = "buyer", source = "buyer") // User -> UserInfo 자동 호출
    @Mapping(target = "productOption", source = ".") // Order -> ProductOptionInfo 자동 호출
    ReviewDetailResponseDto.OrderInfo toOrderInfo(Order order);

    /*
     * =====================
     * User -> UserInfo
     * =====================
     */
    ReviewDetailResponseDto.UserInfo toUserInfo(User user);

    /*
     * =====================
     * Order (Snapshot) -> ProductOptionInfo
     * DTO: id, productOptionName, product
     * =====================
     */
    @Mapping(target = "id", source = "productOptionId")
    @Mapping(target = "productOptionName", source = "productOptionName")
    @Mapping(target = "product", source = ".") // Order -> ProductInfo 자동 호출
    ReviewDetailResponseDto.ProductOptionInfo toProductOptionInfo(Order order);

    /*
     * =====================
     * Order (Snapshot) -> ProductInfo
     * DTO: id, name, productImageUrl
     * =====================
     */
    @Mapping(target = "id", source = "productId")
    @Mapping(target = "name", source = "productName")
    @Mapping(target = "productImageUrl", source = "productImageUrl")
    ReviewDetailResponseDto.ProductInfo toProductInfo(Order order);
}