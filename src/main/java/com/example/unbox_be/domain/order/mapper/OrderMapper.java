package com.example.unbox_be.domain.order.mapper;

import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct Mapper
 * - componentModel = "spring": 스프링 빈으로 등록 (@Component 불필요)
 * - unmappedTargetPolicy = IGNORE: 매핑되지 않은 필드가 있어도 에러 무시 (필요 시 WARN/ERROR 변경)
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    /**
     * 목록 조회용 DTO 변환
     * - source: Entity의 경로 (점.으로 깊은 접근 가능)
     * - target: DTO의 필드명
     */
    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "productOption.product.brand.name", target = "brandName")
    @Mapping(source = "productOption.product.name", target = "productName")
    @Mapping(source = "productOption.option", target = "size")
    @Mapping(source = "productOption.product.imageUrl", target = "imageUrl")
    OrderResponseDto toResponseDto(Order order);

    /**
     * 상세 조회용 DTO 변환 (Nested DTO 매핑)
     * - 내부 객체(product, productOption)는 아래 정의된 메서드를 자동으로 찾아 사용함
     */
    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "productOption", target = "productOption") // 아래 toProductOptionInfo 호출됨
    @Mapping(source = "productOption.product", target = "product") // 아래 toProductInfo 호출됨
    OrderDetailResponseDto toDetailResponseDto(Order order);

    // --- Nested DTO 매핑 메서드 ---

    // ProductOption -> OrderDetailResponseDto.ProductOptionInfo
    @Mapping(source = "id", target = "id")
    @Mapping(source = "option", target = "size")
    OrderDetailResponseDto.ProductOptionInfo toProductOptionInfo(ProductOption productOption);

    // Product -> OrderDetailResponseDto.ProductInfo
    @Mapping(source = "id", target = "id")
    @Mapping(source = "brand.name", target = "brandName")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "modelNumber", target = "modelNumber")
    @Mapping(source = "imageUrl", target = "imageUrl")
    OrderDetailResponseDto.ProductInfo toProductInfo(Product product);
}