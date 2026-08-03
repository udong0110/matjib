package com.example.matjib.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Review {
    private Long reviewId;
    private Long memberId;
    private Long storeId;
    private String title;
    private String content;
    private Integer rating;
    private String imagePath;   // 첨부 이미지 경로
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
