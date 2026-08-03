package com.example.matjib.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * review + member + store 3개 테이블 JOIN 결과를 담는 응답 DTO
 */
@Getter
@Setter
@Schema(description = "리뷰 응답(작성자/가게 정보 포함)")
public class ReviewResponse {
    private Long reviewId;
    private String title;
    private String content;
    private Integer rating;
    private String imagePath;
    private LocalDateTime createdAt;

    // member 조인
    private Long memberId;
    private String writerNickname;

    // store 조인
    private Long storeId;
    private String storeName;
    private String category;
    private String region;
}
