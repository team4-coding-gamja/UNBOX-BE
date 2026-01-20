package com.example.unbox_trade.common.client.product;

import com.example.unbox_trade.common.client.product.dto.ProductOptionForOrderInfoResponse;
import com.example.unbox_trade.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductClient {

    @GetMapping("/internal/products/options/{id}/for-order")
    ProductOptionForOrderInfoResponse getProductForOrder (@PathVariable UUID id);

    @GetMapping("/internal/products/options/{id}/for-sellingbid")
    ProductOptionForSellingBidInfoResponse getProductOptionForSellingBid (@PathVariable UUID id);
}
