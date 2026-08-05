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

    List<StoreListItem> findStores(StoreSearch search);
    long countStores(StoreSearch search);
    void updateImage(@Param("storeId") Long storeId, @Param("imagePath") String imagePath);

    // 별점 4.0 이상 중 리뷰 많은 순
    List<StoreListItem> findBestStores(@Param("region") String region,
                                       @Param("minRating") double minRating,
                                       @Param("limit") int limit);

    // 코스 추천용, seed로 정렬해서 1곳만 뽑음 (seed가 같으면 결과도 같음)
    StoreListItem findCourseStore(@Param("region") String region,
                                  @Param("categories") List<String> categories,
                                  @Param("excludeStoreIds") List<Long> excludeStoreIds,
                                  @Param("franchisePattern") String franchisePattern,
                                  @Param("minRating") Double minRating,
                                  @Param("seed") long seed);
    List<String> findCourseRegions();
}
