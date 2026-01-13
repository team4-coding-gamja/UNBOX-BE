package com.example.unbox_be.global.client.product;

import com.example.unbox_be.global.client.product.dto.ProductOptionForOrderInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// @FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/internal/products/options/{id}/for-order")
    ProductOptionForOrderInfoResponse getProductForOrder(@PathVariable UUID id);
}
