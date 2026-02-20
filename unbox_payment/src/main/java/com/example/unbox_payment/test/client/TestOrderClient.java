package com.example.unbox_payment.test.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "test-order-service", contextId = "testOrderClient", url = "${test-order-service.url}", path = "/order")
public interface TestOrderClient {

    @PostMapping("/internal/orders/{id}/pending-shipment")
    void pendingShipmentOrder(
            @PathVariable("id") UUID id,
            @RequestParam("paymentId") UUID paymentId,
            @RequestParam("updatedBy") String updatedBy,
            @RequestHeader(value = "X-Fault-Target", required = false) String faultTarget,
            @RequestHeader(value = "X-Fault-Delay-MS", required = false) long delayMs);
}
