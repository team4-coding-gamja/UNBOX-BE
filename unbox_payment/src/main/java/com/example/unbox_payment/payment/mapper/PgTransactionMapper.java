package com.example.unbox_payment.payment.mapper;

import com.example.unbox_payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_payment.payment.entity.Payment;
import com.example.unbox_payment.payment.entity.PgTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PgTransactionMapper {

    // ✅ 결제 성공 시 PgTransaction Entity 생성
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "pgPaymentKey", source = "response.paymentKey")
    @Mapping(target = "pgApproveNo", source = "response.approveNo")
    @Mapping(target = "pgProvider", source = "response.method")
    @Mapping(target = "eventType", constant = "PAYMENT")
    @Mapping(target = "eventStatus", constant = "DONE")
    @Mapping(target = "eventAmount", source = "response.totalAmount")
    @Mapping(target = "rawPayload", source = "response.rawJson")
    @Mapping(target = "pgSellerKey", source = "pgSellerKey")
    PgTransaction toSuccessEntity(Payment payment, TossConfirmResponse response, String pgSellerKey);

    // ✅ 결제 실패 시 PgTransaction Entity 생성
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "pgPaymentKey", source = "response.paymentKey")
    @Mapping(target = "pgApproveNo", ignore = true)
    @Mapping(target = "pgProvider", ignore = true)
    @Mapping(target = "eventType", constant = "PAYMENT")
    @Mapping(target = "eventStatus", constant = "FAILED")
    @Mapping(target = "eventAmount", source = "response.totalAmount")
    @Mapping(target = "rawPayload", expression = "java(response != null && response.getRawJson() != null ? response.getRawJson() : \"API Response is Null\")")
    @Mapping(target = "pgSellerKey", ignore = true)
    PgTransaction toFailedEntity(Payment payment, TossConfirmResponse response);
}
