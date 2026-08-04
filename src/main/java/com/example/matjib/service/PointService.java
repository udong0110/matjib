package com.example.matjib.service;

import com.example.matjib.domain.Member;
import com.example.matjib.domain.PointHistory;
import com.example.matjib.exception.BusinessException;
import com.example.matjib.mapper.MemberMapper;
import com.example.matjib.mapper.PointHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final MemberMapper memberMapper;
    private final PointHistoryMapper pointHistoryMapper;

    public static final int REVIEW_REWARD = 100;
    public static final int REFUND_UNIT = 3000;      // 3000점 = 3000원 (1:1)

    /**
     * 리뷰 작성 시 포인트 적립.
     * 회원 포인트 +100 과 내역 기록을 한 트랜잭션으로 묶는다.
     * (ReviewService.create 안에서 호출되어 리뷰 저장과 함께 롤백/커밋된다)
     */
    @Transactional
    public void earnForReview(Long memberId) {
        earn(memberId, REVIEW_REWARD, "리뷰 작성");
    }

    private void earn(Long memberId, int amount, String reason) {
        memberMapper.addPoint(memberId, amount);
        pointHistoryMapper.insert(PointHistory.builder()
                .memberId(memberId)
                .amount(amount)
                .type("EARN")
                .reason(reason)
                .build());
        log.info("포인트 적립: memberId={}, +{} ({})", memberId, amount, reason);
    }

    /**
     * 포인트 환급 신청.
     * 3000점 이상 보유 시에만 가능하며, 3000점을 차감하고 환급 내역을 남긴다.
     * (실제 송금은 관리자가 환급 내역을 보고 처리하는 개념)
     */
    @Transactional
    public void refund(Long memberId) {
        Member member = memberMapper.findById(memberId);
        if (member == null) {
            throw BusinessException.notFound("회원을 찾을 수 없습니다.");
        }
        if (member.getPoint() < REFUND_UNIT) {
            throw BusinessException.conflict(
                    "환급은 " + REFUND_UNIT + "점 이상부터 가능합니다. (현재 " + member.getPoint() + "점)");
        }

        memberMapper.addPoint(memberId, -REFUND_UNIT);
        pointHistoryMapper.insert(PointHistory.builder()
                .memberId(memberId)
                .amount(-REFUND_UNIT)
                .type("REFUND")
                .reason("포인트 환급 (" + REFUND_UNIT + "원)")
                .build());
        log.info("포인트 환급: memberId={}, -{}", memberId, REFUND_UNIT);
    }

    public List<PointHistory> getHistory(Long memberId) {
        return pointHistoryMapper.findByMemberId(memberId);
    }
}
