package com.example.matjib.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "가게 검색/페이징 조건")
public class StoreSearch {

    @Schema(description = "가게 이름 검색", example = "돼지국밥")
    private String keyword;

    @Schema(description = "지역 필터", example = "부산 해운대구")
    private String region;

    @Schema(description = "카테고리 필터", example = "한식")
    private String category;

    @Schema(description = "페이지 번호(1부터)", example = "1")
    private int page = 1;

    @Schema(description = "페이지당 개수", example = "10")
    private int size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }
}
