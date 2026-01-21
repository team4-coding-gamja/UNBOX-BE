package com.example.unbox_user.order.order.mapper;

import com.example.unbox_user.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_user.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_user.order.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderClientMapper {

    @Mapping(target = "buyerId", source = "buyer.id") // USER 분리하면 수정
    @Mapping(target = "buyerNickname", source = "buyer.nickname") // USER 분리하면 수정
    @Mapping(target = "orderStatus", expression = "java(order.getStatus().name())")
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "productName", source = "productName")
    @Mapping(target = "modelNumber", source = "modelNumber")
    @Mapping(target = "productImageUrl", source = "productImageUrl")
    @Mapping(target = "productOptionId", source = "productOptionId")
    @Mapping(target = "productOptionName", source = "productOptionName")
    @Mapping(target = "brandName", source = "brandName")
    OrderForReviewInfoResponse toOrderForReviewInfoResponse(Order order);

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "sellingBidId", source = "sellingBidId")
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "sellerId", source = "seller.id")
    OrderForPaymentInfoResponse toOrderForPaymentInfoResponse(Order order);
}
