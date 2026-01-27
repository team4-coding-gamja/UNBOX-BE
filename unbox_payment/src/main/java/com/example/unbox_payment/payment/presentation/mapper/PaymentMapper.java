package com.example.unbox_payment.payment.mapper;

import com.example.unbox_payment.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_payment.payment.dto.response.PaymentHistoryResponseDto;
import com.example.unbox_payment.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_payment.payment.entity.Payment;
import com.example.unbox_payment.payment.entity.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    // ✅ OrderForPaymentInfoResponse → Payment Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", source = "orderInfo.orderId")
    @Mapping(target = "buyerId", source = "orderInfo.buyerId")
    @Mapping(target = "sellerId", source = "orderInfo.sellerId")
    @Mapping(target = "amount", source = "orderInfo.price")
    @Mapping(target = "sellingBidId", source = "orderInfo.sellingBidId")
    @Mapping(target = "method", source = "method")
    @Mapping(target = "status", constant = "READY")
    @Mapping(target = "readyAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "paymentKey", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Payment toEntity(OrderForPaymentInfoResponse orderInfo, PaymentMethod method);

    // ✅ Payment + OrderInfo → PaymentReadyResponseDto
    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "orderId", source = "orderInfo.orderId")
    @Mapping(target = "price", source = "orderInfo.price")
    PaymentReadyResponseDto toReadyResponseDto(Payment payment, OrderForPaymentInfoResponse orderInfo);

    // ✅ Payment → PaymentHistoryResponseDto
    @Mapping(target = "paymentId", source = "id")
    PaymentHistoryResponseDto toHistoryResponseDto(Payment payment);
}
