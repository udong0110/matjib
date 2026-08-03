package com.example.matjib.controller;

import com.example.matjib.dto.*;
import com.example.matjib.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "리뷰", description = "맛집 리뷰 CRUD / 검색 / 페이징 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 목록 조회",
            description = "키워드/카테고리/지역/최소별점 조건으로 검색하고 페이징합니다. (MyBatis 동적쿼리 + 3테이블 조인)")
    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> list(@ModelAttribute ReviewSearch search) {
        return ResponseEntity.ok(reviewService.getReviews(search));
    }

    @Operation(summary = "리뷰 상세 조회", description = "조회 시 조회수가 1 증가합니다.")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> detail(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReview(reviewId));
    }

    @Operation(summary = "리뷰 작성", description = "로그인한 회원만 작성할 수 있습니다.")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails user) {
        Long id = reviewService.create(request, user.getUsername(), null);
        return ResponseEntity.ok(Map.of("reviewId", id, "message", "리뷰가 등록되었습니다."));
    }
}
