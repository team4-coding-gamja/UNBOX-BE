package com.example.unbox_be.domain.product.product.controller;

import com.example.unbox_be.domain.product.product.service.ProductService;
import com.example.unbox_be.global.client.product.dto.ProductOptionForOrderInfoResponse;
import com.example.unbox_be.global.client.product.dto.ProductOptionForSellingBidInfoResponse;
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
    public CustomApiResponse<ProductOptionForOrderInfoResponse> getProductOptionForOrder(
            @PathVariable UUID id) {
        return CustomApiResponse.success(productService.getProductOptionForOrder(id));
    }

    @GetMapping("/bids/selling/{id}/for-selling-bid")
    public ProductOptionForSellingBidInfoResponse getProductForSellingBid(@PathVariable UUID id) {
        return productService.getProductOptionForSellingBid(id);
    }
}
