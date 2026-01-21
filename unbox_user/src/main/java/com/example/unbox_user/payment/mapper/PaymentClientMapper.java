package com.example.unbox_user.payment.mapper;

import com.example.unbox_user.common.client.payment.dto.PaymentForSettlementResponse;
import com.example.unbox_user.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentClientMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "status", expression = "java(payment.getStatus().name())")
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "amount", source = "amount")
    PaymentForSettlementResponse toPaymentForSettlementResponse(Payment payment);
}