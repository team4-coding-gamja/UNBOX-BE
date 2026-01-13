package com.example.unbox_be.domain.product.controller;

import com.example.unbox_be.domain.product.service.ProductService;
import com.example.unbox_be.global.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final ProductService productService;

    // ✅ 상품 옵션 조회 (주문용)
    @GetMapping("/options/{id}/for-order")
    public CustomApiResponse<com.example.unbox_be.global.client.product.dto.ProductOptionForOrderInfoResponse> getProductOptionForOrder(
            @PathVariable UUID id) {
        return CustomApiResponse.success(productService.getProductOptionForOrder(id));
    }
}
