package com.example.unbox_be.domain.product.mapper;

import com.example.unbox_be.domain.product.dto.ProductRequestDto;
import com.example.unbox_be.domain.product.entity.ProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring", // 스프링 빈으로 등록
        unmappedTargetPolicy = ReportingPolicy.IGNORE // 매핑 안 된 필드 경고 무시
)
public interface ProductRequestMapper {

    // DTO + UserId -> Entity 변환
    @Mapping(target = "id", ignore = true)      // DB 자동 생성
    @Mapping(target = "status", ignore = true)  // 엔티티 생성자에서 PENDING 설정됨
    @Mapping(target = "userId", source = "userId") // 파라미터로 받은 userId 매핑
    // 나머지 필드(name)는 이름이 같아서 자동 매핑됨
    ProductRequest toEntity(ProductRequestDto dto, Long userId);
}