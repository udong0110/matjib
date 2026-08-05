package com.example.matjib.mapper;

import com.example.matjib.domain.Store;
import com.example.matjib.dto.StoreListItem;
import com.example.matjib.dto.StoreSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StoreMapper {

    // 프랜차이즈 이름 패턴, 지역 랭킹/코스 추천에서 후보 제외할 때 공통으로 씀
    String FRANCHISE_PATTERN =
            "스타벅스|투썸|이디야|메가|빽다방|컴포즈|커피빈|폴바셋|엔제리너스|" +
            "맥도날드|롯데리아|버거킹|맘스터치|KFC|서브웨이|" +
            "본죽|김밥천국|한솥|명랑|BBQ|BHC|교촌|굽네|네네|처갓집|페리카나|" +
            "파리바게뜨|뚜레쥬르|배스킨|던킨";

    List<Store> findAll();
    Store findById(@Param("storeId") Long storeId);
    int countByNameAndAddress(Store store);   // 시딩 시 중복 체크
    void insert(Store store);

    List<StoreListItem> findStores(StoreSearch search);
    long countStores(StoreSearch search);
    void updateImage(@Param("storeId") Long storeId, @Param("imagePath") String imagePath);

    // 별점 4.0 이상 중 평균 별점 높은 순, 프랜차이즈 제외
    List<StoreListItem> findBestStores(@Param("region") String region,
                                       @Param("minRating") double minRating,
                                       @Param("limit") int limit,
                                       @Param("franchisePattern") String franchisePattern);

    // 코스 추천용, seed로 정렬해서 1곳만 뽑음 (seed가 같으면 결과도 같음)
    StoreListItem findCourseStore(@Param("region") String region,
                                  @Param("categories") List<String> categories,
                                  @Param("excludeStoreIds") List<Long> excludeStoreIds,
                                  @Param("franchisePattern") String franchisePattern,
                                  @Param("minRating") Double minRating,
                                  @Param("seed") long seed);
    List<String> findCourseRegions();
}
