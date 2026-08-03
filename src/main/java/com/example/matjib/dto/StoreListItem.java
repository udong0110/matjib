package com.example.matjib.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "가게 목록 아이템 (리뷰 수/평균 별점 포함)")
public class StoreListItem {
    private Long storeId;
    private String storeName;
    private String category;
    private String region;
    private String address;
    private String imagePath;
    private Double latitude;
    private Double longitude;
    private Double avgRating;    // 평균 별점 (리뷰 없으면 null)
    private Integer reviewCount; // 리뷰 수
    private String oneLiner;     // 한줄소개 (최신 리뷰 제목)
}
