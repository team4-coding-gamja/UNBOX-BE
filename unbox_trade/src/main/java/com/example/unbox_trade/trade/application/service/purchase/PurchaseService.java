package com.example.unbox_trade.trade.application.service.purchase;

import java.util.UUID;

public interface PurchaseService {
    void purchase(UUID sellingBidId, Long buyerId);
}
