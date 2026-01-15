package com.example.unbox_be.domain.product.reviews.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewUpdateRequestDto {

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 1000, message = "리뷰 내용은 최대 1000자까지 가능합니다.")
    private String content;

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5 이하여야 합니다.")
    private Integer rating;

    // 선택값: null 허용. 값이 들어오면 URL 형식 검사
    @URL(message = "imageUrl은 올바른 URL 형식이어야 합니다.")
    private String imageUrl;
}
