package com.example.matjib.mapper;

import com.example.matjib.domain.Review;
import com.example.matjib.dto.ReviewResponse;
import com.example.matjib.dto.ReviewSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReviewMapper {
    // 동적쿼리 + 3테이블 조인 + 페이징
    List<ReviewResponse> findReviews(ReviewSearch search);
    long countReviews(ReviewSearch search);

    ReviewResponse findById(@Param("reviewId") Long reviewId);   // 상세(조인)
    Review findEntityById(@Param("reviewId") Long reviewId);      // 작성자 검증용
    List<ReviewResponse> findByStoreId(@Param("storeId") Long storeId);   // 가게별 리뷰
    void insert(Review review);
    void update(Review review);
    void delete(@Param("reviewId") Long reviewId);
    int countByMemberId(@Param("memberId") Long memberId);   // 회원별 리뷰 수
}
