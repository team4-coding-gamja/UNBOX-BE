package com.example.unbox_be.domain.trade.event;

import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.global.event.product.BrandDeletedEvent;
import com.example.unbox_be.global.event.product.ProductDeletedEvent;
import com.example.unbox_be.global.event.product.ProductOptionDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TradeEventListener {

    private final SellingBidService sellingBidService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBrandDeleted(BrandDeletedEvent event) {

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(ProductDeletedEvent event) {

        SellingBidService.deleteSellingBid(event.productId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductOptionDeleted(ProductOptionDeletedEvent event) {



    }
}
