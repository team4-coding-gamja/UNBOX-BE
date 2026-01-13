package com.example.unbox_be.domain.reviews.mapper;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.user.entity.User;
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
     * =====================
     */
    @Mapping(target = "order", source = "order")
    ReviewDetailResponseDto toReviewDetailResponseDto(Review review);

    /*
     * =====================
     * 리뷰 리스트 응답
     * =====================
     */
    @Mapping(target = "buyerNickname", source = "order.buyer.nickname")
    @Mapping(target = "productOptionName", source = "order.optionName")
    ReviewListResponseDto toReviewListResponseDto(Review review);

    /*
     * =====================
     * Order -> OrderInfo
     * =====================
     */
    @Mapping(target = "buyer", source = "buyer")
    @Mapping(target = "productOption", source = ".") // calls toProductOptionInfo(Order)
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
     * =====================
     */
    @Mapping(target = "id", source = "productOptionId")
    @Mapping(target = "option", source = "optionName")
    @Mapping(target = "product", source = ".") // calls toProductInfo(Order)
    ReviewDetailResponseDto.ProductOptionInfo toProductOptionInfo(Order order);

    /*
     * =====================
     * Order (Snapshot) -> ProductInfo
     * =====================
     */
    @Mapping(target = "id", source = "productId")
    @Mapping(target = "name", source = "productName")
    @Mapping(target = "imageUrl", source = "imageUrl")
    ReviewDetailResponseDto.ProductInfo toProductInfo(Order order);
}