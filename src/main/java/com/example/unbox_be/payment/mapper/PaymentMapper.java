package com.example.unbox_be.payment.mapper;

import com.example.unbox_be.order.order.entity.Order;
import com.example.unbox_be.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.payment.entity.Payment;
import com.example.unbox_be.payment.entity.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    // ✅ Order -> Payment Entity 변환
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "amount", source = "order.price")
    @Mapping(target = "method", source = "method")
    @Mapping(target = "status", constant = "READY")
    @Mapping(target = "pgPaymentKey", ignore = true)
    @Mapping(target = "pgApproveNo", ignore = true)
    @Mapping(target = "capturedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Payment toEntity(Order order, PaymentMethod method);

    // ✅ Payment Entity -> PaymentReadyResponseDto 변환
    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "price", source = "order.price")
    PaymentReadyResponseDto toReadyResponseDto(Payment payment, Order order);
}
