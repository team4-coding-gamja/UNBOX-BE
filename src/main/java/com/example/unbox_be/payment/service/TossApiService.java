package com.example.unbox_be.payment.service;

import com.example.unbox_be.payment.dto.response.TossConfirmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class TossApiService {

    public TossConfirmResponse confirm(String paymentKey, BigDecimal amount) {
        String confirmedKey = paymentKey + "_CONFIRMED_" + UUID.randomUUID().toString().substring(0, 4);
        return TossConfirmResponse.builder()
                .paymentKey(paymentKey)
                .status("DONE") // 성공 테스트용
                .orderId("test-order-id")
                .approveNo(confirmedKey)
                .method("TOSS")
                .totalAmount(amount)
                .rawJson("{\"status\":\"DONE\"}")
                .build();
    }
    public void cancel(String paymentKey, String cancelReason) {
        try {
            log.info("PG 결제 취소 요청 발송 - Key: {}, 사유: {}", paymentKey, cancelReason);
            log.info("PG 결제 취소 완료");
        } catch (Exception e) {
            log.error("PG 결제 취소 요청 중 오류 발생! 수동 확인 필요: {}", e.getMessage());
        }
    }
}