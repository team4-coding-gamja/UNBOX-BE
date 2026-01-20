package com.example.unbox_user.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossConfirmResponse {
    private String paymentKey;
    private String orderId;
    private BigDecimal totalAmount;
    private String method;
    private String status;
    private String requestedAt;
    private String approveNo;
    private String approvedAt;
    private String rawJson;

    // setter 대신 가독성 좋은 메서드명 사용
    public void fillRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public boolean isSuccess() {
        // null 방어를 위해 status가 앞이 아닌 DONE 문자열을 앞에 둠
        return "DONE".equals(this.status);
    }
}