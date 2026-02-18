package com.example.unbox_product.product.presentation.dto.response;

import com.example.unbox_product.product.domain.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Redis에 객체 자체를 저장하기 위해 Serializable 인터페이스를 추가하는 것이 좋습니다.
public class ProductListResponseDtoV2 implements Serializable {

    private UUID productId;
    private String productName;
    private String modelNumber;
    private Category category;
    private String productImageUrl;

    private UUID brandId;
    private String brandName;

    // v2 추가: 다음 페이지 조회를 위한 커서(Cursor) 데이터
    private Long popularityScore;
}