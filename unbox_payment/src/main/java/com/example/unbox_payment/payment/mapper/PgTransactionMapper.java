package com.example.unbox_payment.payment.mapper;

import com.example.unbox_payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_payment.payment.entity.Payment;
import com.example.unbox_payment.payment.entity.PgTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PgTransactionMapper {

    // ✅ 결제 성공 시 PgTransaction Entity 생성
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "transactionKey", expression = "java(generateTransactionKey(response.getPaymentKey(), \"PAYMENT\"))")
    @Mapping(target = "paymentKey", source = "response.paymentKey")
    @Mapping(target = "orderId", source = "payment.orderId")
    @Mapping(target = "method", source = "response.method")
    @Mapping(target = "customerKey", expression = "java(generateCustomerKey(payment.getBuyerId()))")
    @Mapping(target = "status", constant = "DONE")
    @Mapping(target = "amount", source = "response.totalAmount")
    @Mapping(target = "transactionAt", expression = "java(parseTransactionAt(response.getApprovedAt()))")
    @Mapping(target = "currency", constant = "KRW")
    @Mapping(target = "receiptUrl", ignore = true)
    @Mapping(target = "useEscrow", constant = "false")
    @Mapping(target = "rawResponse", source = "response.rawJson")
    PgTransaction toSuccessEntity(Payment payment, TossConfirmResponse response);

    // ✅ 결제 실패 시 PgTransaction Entity 생성
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "transactionKey", expression = "java(generateTransactionKey(response.getPaymentKey(), \"PAYMENT_FAILED\"))")
    @Mapping(target = "paymentKey", source = "response.paymentKey")
    @Mapping(target = "orderId", source = "payment.orderId")
    @Mapping(target = "method", source = "response.method")
    @Mapping(target = "customerKey", expression = "java(generateCustomerKey(payment.getBuyerId()))")
    @Mapping(target = "status", constant = "FAILED")
    @Mapping(target = "amount", source = "response.totalAmount")
    @Mapping(target = "transactionAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "currency", constant = "KRW")
    @Mapping(target = "receiptUrl", ignore = true)
    @Mapping(target = "useEscrow", constant = "false")
    @Mapping(target = "rawResponse", expression = "java(response != null && response.getRawJson() != null ? response.getRawJson() : \"API Response is Null\")")
    PgTransaction toFailedEntity(Payment payment, TossConfirmResponse response);

    // ✅ Helper 메서드들
    default String generateTransactionKey(String paymentKey, String type) {
        if (paymentKey == null) {
            paymentKey = "UNKNOWN";
        }
        return paymentKey + "_" + type + "_" + System.currentTimeMillis();
    }

    default String generateCustomerKey(Long buyerId) {
        // 토스 customerKey 형식: UUID 또는 충분히 무작위적인 값
        return "BUYER_" + buyerId + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    default LocalDateTime parseTransactionAt(String approvedAt) {
        if (approvedAt == null || approvedAt.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(approvedAt);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
