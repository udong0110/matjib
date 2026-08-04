package com.example.matjib.service;

import com.example.matjib.domain.Member;
import com.example.matjib.domain.Review;
import com.example.matjib.dto.*;
import com.example.matjib.exception.BusinessException;
import com.example.matjib.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final MemberService memberService;
    private final PointService pointService;
    private final VisitService visitService;

    // 목록 + 검색 + 페이징
    public PageResponse<ReviewResponse> getReviews(ReviewSearch search) {
        List<ReviewResponse> content = reviewMapper.findReviews(search);
        long total = reviewMapper.countReviews(search);
        log.debug("리뷰 목록 조회: 조건={}, 결과={}건 / 전체={}건",
                search.getKeyword(), content.size(), total);
        return new PageResponse<>(content, search.getPage(), search.getSize(), total);
    }

    // 상세
    public ReviewResponse getReview(Long reviewId) {
        ReviewResponse review = reviewMapper.findById(reviewId);
        if (review == null) {
            throw BusinessException.notFound("리뷰를 찾을 수 없습니다. id=" + reviewId);
        }
        return review;
    }

    // 가게별 리뷰 목록 (가게 상세 페이지용)
    public List<ReviewResponse> getReviewsByStore(Long storeId) {
        return reviewMapper.findByStoreId(storeId);
    }

    @Transactional
    public Long create(ReviewRequest request, String username, String imagePath) {
        Member writer = memberService.getByUsername(username);

        // 방문 인증한 사람만 리뷰 작성 가능
        if (!visitService.hasVisited(writer.getMemberId(), request.getStoreId())) {
            throw BusinessException.forbidden("방문 인증을 한 가게에만 리뷰를 쓸 수 있습니다.");
        }

        Review review = new Review();
        review.setMemberId(writer.getMemberId());
        review.setStoreId(request.getStoreId());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        review.setRating(request.getRating());
        review.setImagePath(imagePath);

        reviewMapper.insert(review);

        // 리뷰 작성 보상: 100P (같은 트랜잭션 → 실패하면 전부 롤백)
        pointService.earnForReview(writer.getMemberId());

        log.info("리뷰 작성: reviewId={}, writer={}", review.getReviewId(), username);
        return review.getReviewId();
    }

    // 리뷰 삭제 (관리자 전용) — 부적절한 리뷰 정리용.
    // 사용자 삭제는 막음: 포인트 어뷰징(썼다 지웠다 반복, 환급 후 삭제) 방지.
    @Transactional
    public void deleteByAdmin(Long reviewId) {
        Review review = reviewMapper.findEntityById(reviewId);
        if (review == null) {
            throw BusinessException.notFound("리뷰를 찾을 수 없습니다. id=" + reviewId);
        }
        reviewMapper.delete(reviewId);
        log.info("리뷰 삭제(관리자): reviewId={}", reviewId);
    }
}
