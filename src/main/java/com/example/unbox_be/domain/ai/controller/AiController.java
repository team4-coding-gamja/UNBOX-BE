package com.example.unbox_be.domain.ai.controller;

import com.example.unbox_be.domain.ai.controller.api.AiApi;
import com.example.unbox_be.domain.ai.dto.response.AiReviewSummaryResponseDto;
import com.example.unbox_be.domain.ai.service.AiService;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController implements AiApi {

    private final AiService aiService;

    @GetMapping("/reviews/summary/{productId}")
    public CustomApiResponse<AiReviewSummaryResponseDto> getReviewSummary(
            @PathVariable UUID productId) {
        AiReviewSummaryResponseDto result = aiService.summarizeReviews(productId);
        return CustomApiResponse.success(result);
    }
}
