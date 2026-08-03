package com.example.matjib.mapper;

import com.example.matjib.domain.PointHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PointHistoryMapper {
    void insert(PointHistory history);
    List<PointHistory> findByMemberId(@Param("memberId") Long memberId);
}
