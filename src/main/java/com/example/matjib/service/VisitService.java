package com.example.matjib.service;

import com.example.matjib.domain.Member;
import com.example.matjib.domain.Visit;
import com.example.matjib.mapper.VisitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitMapper visitMapper;
    private final MemberService memberService;

    // 방문 인증 (해당 가게를 방문했다고 기록)
    @Transactional
    public void certify(Long storeId, String username) {
        Member member = memberService.getByUsername(username);
        // 이미 인증했으면 중복 기록 안 함
        if (visitMapper.countByMemberAndStore(member.getMemberId(), storeId) == 0) {
            visitMapper.insert(Visit.builder()
                    .memberId(member.getMemberId())
                    .storeId(storeId)
                    .build());
            log.info("방문 인증: memberId={}, storeId={}", member.getMemberId(), storeId);
        }
    }

    // 방문 인증 여부 확인
    public boolean hasVisited(Long memberId, Long storeId) {
        return visitMapper.countByMemberAndStore(memberId, storeId) > 0;
    }
}
