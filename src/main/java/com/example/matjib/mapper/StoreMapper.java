package com.example.matjib.mapper;

import com.example.matjib.domain.Store;
import com.example.matjib.dto.StoreListItem;
import com.example.matjib.dto.StoreSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StoreMapper {
    List<Store> findAll();
    Store findById(@Param("storeId") Long storeId);
    int countByNameAndAddress(Store store);   // 시딩 시 중복 체크
    void insert(Store store);

    // 가게 목록 (이름 검색 + 지역 필터 + 페이징, 리뷰 수/평균별점 조인)
    List<StoreListItem> findStores(StoreSearch search);
    long countStores(StoreSearch search);
    void updateImage(@Param("storeId") Long storeId, @Param("imagePath") String imagePath);

    // 코스 추천용
    StoreListItem findCourseStore(@Param("region") String region,
                                  @Param("categories") List<String> categories,
                                  @Param("excludeStoreIds") List<Long> excludeStoreIds,
                                  @Param("franchisePattern") String franchisePattern);
    List<String> findCourseRegions();
}
