package com.example.unbox_common.config;

import com.example.unbox_common.error.ErrorResponse;
import com.example.unbox_common.error.exception.FeignClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class GlobalFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        String bodyStr = "Unknown Error";
        try {
            if (response.body() != null) {
                try (InputStream is = response.body().asInputStream()) {
                    bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                ErrorResponse errorResponse = objectMapper.readValue(bodyStr, ErrorResponse.class);
                return new FeignClientException(errorResponse.getStatus(), errorResponse.getMessage(), errorResponse.getData());
            }
        } catch (IOException e) {
            log.error("Feign Error Reading Failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Feign Error Response Parsing Failed. Body: {}, Error: {}", bodyStr, e.getMessage());
        }

        // 파싱 실패 또는 예상치 못한 에러 시 기본 예외
        return new FeignClientException(response.status(), "External API Error (" + response.status() + "): " + response.reason());
    }
}
