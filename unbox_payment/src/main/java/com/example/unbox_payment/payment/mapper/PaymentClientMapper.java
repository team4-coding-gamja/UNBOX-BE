package com.example.unbox_payment.payment.mapper;

import com.example.unbox_payment.payment.dto.internal.PaymentForSettlementResponse;
import com.example.unbox_payment.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentClientMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "status", expression = "java(payment.getStatus().name())")
    @Mapping(target = "orderId", expression = "java(java.util.UUID.fromString(payment.getOrderId()))")
    @Mapping(target = "sellerId", source = "sellerId")
    @Mapping(target = "amount", source = "amount")
    PaymentForSettlementResponse toPaymentForSettlementResponse(Payment payment);
}