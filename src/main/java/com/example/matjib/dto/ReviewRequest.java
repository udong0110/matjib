package com.example.matjib.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "리뷰 작성/수정 요청")
public class ReviewRequest {

    @NotNull(message = "가게를 선택하세요.")
    @Schema(description = "가게 ID", example = "1")
    private Long storeId;

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "제목", example = "인생 갈비집")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "내용", example = "고기가 정말 부드럽고 밑반찬도 훌륭합니다.")
    private String content;

    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5 이하여야 합니다.")
    @Schema(description = "별점(1~5)", example = "5")
    private Integer rating;
}
