package com.example.unbox_order.settlement.mapper;

import com.example.unbox_order.common.client.settlement.dto.SettlementCreateResponse;
import com.example.unbox_order.common.client.settlement.dto.SettlementForPaymentResponse;
import com.example.unbox_order.settlement.entity.Settlement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SettlementClientMapper {

    // ✅ Settlement → SettlementCreateResponse (생성용)
    @Mapping(target = "settlementId", source = "id")
    @Mapping(target = "settlementStatus", expression = "java(settlement.getStatus().name())")
    SettlementCreateResponse toSettlementCreateResponse(Settlement settlement);

    // ✅ Settlement → SettlementDetailResponse (조회용)
    @Mapping(target = "settlementId", source = "id")
    @Mapping(target = "settlementStatus", expression = "java(settlement.getStatus().name())")
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "paymentId", source = "paymentId")
    @Mapping(target = "sellerId", source = "sellerId")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "feesAmount", source = "feesAmount")
    @Mapping(target = "settlementAmount", source = "payOutAmount")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    SettlementForPaymentResponse toSettlementForPaymentResponse(Settlement settlement);
}