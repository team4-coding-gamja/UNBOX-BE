package com.example.unbox_product.product.presentation.controller;

import com.example.unbox_product.product.application.service.ProductService;
import com.example.unbox_product.product.presentation.dto.internal.ProductOptionForOrderInfoResponse;
import com.example.unbox_product.product.presentation.dto.internal.ProductOptionForSellingBidInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

@Tag(name = "[내부] 상품 관리", description = "내부 시스템용 상품 API")
@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final ProductService productService;

    // ✅ 상품 옵션 조회 (주문용)
    @Operation(summary = "주문용 상품 옵션 정보 조회", description = "주문 처리를 위해 상품 옵션 정보를 조회합니다.")
    @GetMapping("/options/{id}/for-order")
    public ProductOptionForOrderInfoResponse getProductOptionForOrder(
            @PathVariable UUID id) {
        return productService.getProductOptionForOrder(id);
    }

    @Operation(summary = "판매 입찰용 상품 옵션 정보 조회", description = "판매 입찰 등록을 위해 상품 옵션 정보를 조회합니다.")
    @GetMapping("/options/{id}/for-selling-bid")
    public ProductOptionForSellingBidInfoResponse getProductForSellingBid(@PathVariable UUID id) {
        return productService.getProductOptionForSellingBid(id);
    }
}
