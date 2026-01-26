package com.example.unbox_order.order.mapper;

import com.example.unbox_order.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_order.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_order.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderClientMapper {

    // 검증용 필드
    @Mapping(target = "buyerId", source = "buyerId")
    @Mapping(target = "orderStatus", expression = "java(order.getStatus().name())")
    // 스냅샷 저장 필드
    @Mapping(target = "buyerNickname", source = "buyerName")
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
    @Mapping(target = "buyerId", source = "buyerId")
    @Mapping(target = "sellerId", source = "sellerId")
    OrderForPaymentInfoResponse toOrderForPaymentInfoResponse(Order order);
}
