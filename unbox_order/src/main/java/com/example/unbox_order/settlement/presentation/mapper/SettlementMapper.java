package com.example.unbox_order.settlement.presentation.mapper;

import com.example.unbox_order.settlement.presentation.dto.response.SettlementResponseDto;
import com.example.unbox_order.settlement.domain.entity.Settlement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SettlementMapper {

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
    SettlementResponseDto toSettlementResponseDto(Settlement settlement);
}
