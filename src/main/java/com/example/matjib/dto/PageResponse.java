package com.example.matjib.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "페이징 응답")
public class PageResponse<T> {
    private final List<T> content;   // 현재 페이지 데이터
    private final int page;          // 현재 페이지(1부터)
    private final int size;          // 페이지당 개수
    private final long totalCount;   // 전체 개수
    private final int totalPages;    // 전체 페이지 수

    public PageResponse(List<T> content, int page, int size, long totalCount) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalCount = totalCount;
        this.totalPages = (int) Math.ceil((double) totalCount / size);
    }
}
