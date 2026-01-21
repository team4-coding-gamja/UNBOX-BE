package com.example.unbox_user.payment.mapper;

import com.example.unbox_user.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_user.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_user.payment.entity.Payment;
import com.example.unbox_user.payment.entity.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    // ✅ OrderForPaymentInfoResponse용
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", source = "orderInfo.orderId")
    @Mapping(target = "buyerId", source = "orderInfo.buyerId")
    @Mapping(target = "sellerId", source = "orderInfo.sellerId")
    @Mapping(target = "amount", source = "orderInfo.price")
    @Mapping(target = "method", source = "method")
    @Mapping(target = "status", constant = "READY")
    @Mapping(target = "pgPaymentKey", ignore = true)
    @Mapping(target = "pgApproveNo", ignore = true)
    @Mapping(target = "capturedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Payment toEntity(OrderForPaymentInfoResponse orderInfo, PaymentMethod method);

    // ✅ OrderForPaymentInfoResponse용
    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "orderId", source = "orderInfo.orderId")
    @Mapping(target = "price", source = "orderInfo.price")
    PaymentReadyResponseDto toReadyResponseDto(Payment payment, OrderForPaymentInfoResponse orderInfo);
}
