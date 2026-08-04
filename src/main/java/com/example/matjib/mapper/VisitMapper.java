package com.example.matjib.mapper;

import com.example.matjib.domain.Visit;
import org.apache.ibatis.annotations.Param;

public interface VisitMapper {
    void insert(Visit visit);
    int countByMemberAndStore(@Param("memberId") Long memberId, @Param("storeId") Long storeId);
}
