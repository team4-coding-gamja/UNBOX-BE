package com.example.unbox_payment.payment.service;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_payment.payment.dto.response.TossConfirmResponse;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TossApiService {

    @Value("${payment.toss.secret-key}")
    private String secretKey;

    @Value("${payment.toss.confirm-url}")
    private String confirmUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ 토스 결제 승인 API 호출
    public TossConfirmResponse confirm(String paymentKey, UUID orderId, BigDecimal amount, String requestId) {
        try {
            // 1. 헤더 설정 (Basic Auth 및 Idempotency-Key 추가)
            HttpHeaders headers = new HttpHeaders();
            String encodedKey = Base64.getEncoder().encodeToString((secretKey.trim() + ":").getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedKey);
            headers.set("Idempotency-Key", requestId); // 멱등성 키 설정
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. 바디 설정
            Map<String, Object> body = new HashMap<>();
            body.put("paymentKey", paymentKey);
            body.put("orderId", orderId.toString());
            body.put("amount", amount);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 3. API 호출
            log.info("[Toss API Request] URL: {}, orderId: {}, requestId: {}", confirmUrl, orderId, requestId);
            ResponseEntity<TossConfirmResponse> response = restTemplate.postForEntity(confirmUrl, entity,
                    TossConfirmResponse.class);

            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[Toss API Error] Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return TossConfirmResponse.builder()
                    .status("FAILED")
                    .errorCode(e.getStatusCode().toString())
                    .errorMessage(e.getResponseBodyAsString())
                    .build();
        } catch (Exception e) {
            log.error("[Toss API Unknown Error] {}", e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    // ✅ 토스 결제 취소 API 호출
    public void cancel(String paymentKey, String cancelReason, String requestId) {
        try {
            String cancelUrl = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

            HttpHeaders headers = new HttpHeaders();
            String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
            headers.set("Authorization", "Basic " + encodedKey);
            headers.set("Idempotency-Key", requestId); // 취소 시에도 멱등성 키 권장
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("cancelReason", cancelReason);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(cancelUrl, entity, String.class);
            log.info("[Toss API Cancel Success] paymentKey: {}, requestId: {}", paymentKey, requestId);

        } catch (Exception e) {
            log.error("[Toss API Cancel Failed] paymentKey: {}, error: {}", paymentKey, e.getMessage());
        }
    }
}