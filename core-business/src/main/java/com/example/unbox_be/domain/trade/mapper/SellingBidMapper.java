package com.example.unbox_be.domain.trade.mapper;

import com.example.unbox_be.domain.product.entity.ProductOption; // ✅ import 추가
import com.example.unbox_be.domain.trade.dto.request.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SellingBidMapper {

    // 1. DTO -> Entity 변환
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "deadline", source = "deadline")

    // ✅ [핵심 수정] 파라미터로 받은 객체(productOption)를 타겟 필드(productOption)에 그대로 넣음
    @Mapping(source = "productOption", target = "productOption")

    // ❌ [삭제] 기존에 에러 나던 부분 (dto.optionId -> productOption.id 매핑) 삭제
    // @Mapping(source = "dto.optionId", target = "productOption.id")

    SellingBid toEntity(
            SellingBidRequestDto dto,
            Long userId,
            LocalDateTime deadline,
            ProductOption productOption // ✅ 파라미터 추가
    );


    // 2. Entity -> Response DTO 변환 (기존 유지)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "size", ignore = true)
    SellingBidResponseDto toResponseDto(SellingBid entity);
}