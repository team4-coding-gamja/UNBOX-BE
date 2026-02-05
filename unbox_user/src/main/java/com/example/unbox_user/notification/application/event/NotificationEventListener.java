package com.example.unbox_user.notification.application.event;

import com.example.unbox_common.event.trade.BuyingBidMatchedEvent;
import com.example.unbox_user.notification.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "trade-events", groupId = "user-group")
    public void handleBuyingBidMatched(BuyingBidMatchedEvent event) {
        log.info("Received BuyingBidMatchedEvent: {}", event.buyingBidId());

        // 알림 메시지 생성
        String title = "판매자가 나타났습니다!";
        String message = String.format(
                "%s (%s) 상품의 구매 입찰에 판매자가 매칭되었습니다. " +
                        "24시간 내에 결제를 완료해주세요.",
                event.productName(),
                event.productOptionName()
        );

        // 알림 발송 (Push, SMS, Email 등)
        notificationService.sendNotification(
                event.buyerId(),
                title,
                message,
                String.valueOf(NotificationType.BUYING_BID_MATCHED),
                event.buyingBidId().toString()
        );
    }
}
