package com.example.unbox_be.order.mapper;

import com.example.unbox_be.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.order.dto.response.OrderResponseDto;
import com.example.unbox_be.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct Mapper
 * - componentModel = "spring": 스프링 빈으로 등록 (@Component 불필요)
 * - unmappedTargetPolicy = ERROR: 매핑 누락을 컴파일 단계에서 잡기 (권장)
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OrderMapper {

    /**
     * 주문 목록 응답 DTO 변환
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "productOptionName", target = "size")
    @Mapping(source = "productImageUrl", target = "imageUrl")
    OrderResponseDto toResponseDto(Order order);

    /* =========================
     * 주문 상세 응답 DTO 변환
     * - source="." 는 Order 전체를 의미
     * - 아래 toProductOptionInfo / toProductInfo 메서드가 자동으로 선택되어 중첩 객체 생성
     * ========================= */
    @Mapping(source = "id", target = "orderId")
    @Mapping(source = ".", target = "productOptionInfo")
    @Mapping(source = ".", target = "productInfo")
    OrderDetailResponseDto toDetailResponseDto(Order order);

    // Order -> OrderDetailResponseDto.ProductOptionInfo
    @Mapping(source = "productOptionId", target = "id")
    @Mapping(source = "productOptionName", target = "productOptionName")
    OrderDetailResponseDto.ProductOptionInfo toProductOptionInfo(Order order);

    // Order -> OrderDetailResponseDto.ProductInfo
    @Mapping(source = "productId", target = "id")
    @Mapping(source = "brandName", target = "brandName")
    @Mapping(source = "productName", target = "productName")
    @Mapping(source = "modelNumber", target = "modelNumber")
    @Mapping(source = "productImageUrl", target = "productImageUrl")
    OrderDetailResponseDto.ProductInfo toProductInfo(Order order);
}
