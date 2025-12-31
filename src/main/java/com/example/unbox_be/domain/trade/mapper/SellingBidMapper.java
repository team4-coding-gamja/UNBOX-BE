package com.example.unbox_be.domain.trade.mapper;

import com.example.unbox_be.domain.trade.dto.request.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE // 매핑되지 않는 필드가 있어도 경고하지 않음
)
public interface SellingBidMapper {

    // 1. DTO -> Entity 변환
    @Mapping(target = "sellingId", ignore = true) // DB 자동 생성 값이므로 무시
    @Mapping(target = "status", ignore = true)    // 엔티티의 @Builder.Default(LIVE)를 사용하기 위해 무시
    @Mapping(target = "deadline", ignore = true)  // 필요 시 서비스에서 별도로 계산하여 주입하거나 DTO에 추가
    SellingBid toEntity(SellingBidRequestDto dto, LocalDateTime deadline);

    // 2. Entity -> DTO 변환 (나중에 조회가 필요할 때 사용)
    // 현재는 SellingBidResponseDto가 없으므로 주석 처리하거나 필요시 생성하세요.
    // SellingBidResponseDto toResponseDto(SellingBid entity);
}