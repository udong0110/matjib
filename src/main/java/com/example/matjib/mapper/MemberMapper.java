package com.example.matjib.mapper;

import com.example.matjib.domain.Member;
import org.apache.ibatis.annotations.Param;

public interface MemberMapper {
    Member findByUsername(@Param("username") String username);
    Member findById(@Param("memberId") Long memberId);
    int countByUsername(@Param("username") String username);
    void insert(Member member);
    void addPoint(@Param("memberId") Long memberId, @Param("amount") int amount);   // 포인트 증감
}
