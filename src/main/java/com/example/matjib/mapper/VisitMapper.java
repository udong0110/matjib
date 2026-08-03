package com.example.matjib.mapper;

import com.example.matjib.domain.Visit;
import org.apache.ibatis.annotations.Param;

public interface VisitMapper {
    void insert(Visit visit);
    // 특정 회원이 특정 가게를 방문 인증했는지 여부
    int countByMemberAndStore(@Param("memberId") Long memberId, @Param("storeId") Long storeId);
}
