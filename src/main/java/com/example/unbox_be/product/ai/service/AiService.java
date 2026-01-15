package com.example.unbox_be.product.ai.service;

import com.example.unbox_be.product.ai.dto.response.AiReviewSummaryResponseDto;
import com.example.unbox_be.product.reviews.entity.Review;
import com.example.unbox_be.product.reviews.repository.ReviewRepository;
import com.example.unbox_be.common.config.openai.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private final ReviewRepository reviewRepository;
    private final WebClient.Builder webClientBuilder;
    private final OpenAiProperties openAiProperties;

    @Transactional(readOnly = true)
    public AiReviewSummaryResponseDto summarizeReviews(UUID productId) {
        // 1. 해당 상품의 모든 리뷰 조회
        List<Review> reviews = reviewRepository.findAllByOrderProductIdAndDeletedAtIsNull(productId);

        int reviewCount = reviews.size();
        if (reviewCount == 0) {
            return AiReviewSummaryResponseDto.of("아직 리뷰가 없습니다.", productId, 0);
        }

        // 2. 리뷰 내용 수집
        String collectedReviews = reviews.stream()
                .map(Review::getContent)
                .collect(Collectors.joining("\n"));

        // 3. AI 요약 요청
        String summary = callAiApi(collectedReviews);

        // 4. 응답 반환
        return AiReviewSummaryResponseDto.of(summary, productId, reviewCount);
    }

    private String callAiApi(String context) {
        // Prompt construction
        String prompt = "다음 리뷰들을 50자 이하의 한 문장으로 요약해줘:\n\n" + context;

        // Request Body
        Map<String, Object> requestBody = Map.of(
                "model", openAiProperties.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant that summarizes reviews."),
                        Map.of("role", "user", "content", prompt)),
                "max_tokens", 100,
                "temperature", 0.7);

        try {
            // WebClient call
            Map<?, ?> response = webClientBuilder.build()
                    .post()
                    .uri(openAiProperties.baseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + openAiProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // Parse Response
            if (response != null && response.containsKey("choices")) {
                List<?> choices = (List<?>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "AI 서비스 연결 실패 (잠시 후 다시 시도해주세요)";
        }
        return "요약을 생성할 수 없습니다.";
    }
}
