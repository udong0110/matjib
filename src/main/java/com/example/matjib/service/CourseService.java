package com.example.matjib.service;

import com.example.matjib.dto.StoreListItem;
import com.example.matjib.mapper.StoreMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 지역별 맛집 코스 추천.
 * 아침(카페) / 점심(식사류) / 저녁(식사류) 각 1곳씩 — 카페는 아침에만.
 * 유명 맛집만 반복 노출되지 않도록, 별점 3.0 이상인 후보 중에서 랜덤으로 뽑아
 * 지역 상권을 고루 노출한다. 프랜차이즈는 제외하고 로컬 가게를 우선한다.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final StoreMapper storeMapper;

    private static final double MIN_RATING = 3.0;

    // 프랜차이즈(체인) 추정 이름 패턴 — 이 이름이 들어가면 로컬이 아니라고 보고 후보에서 제외
    private static final String FRANCHISE_PATTERN =
            "스타벅스|투썸|이디야|메가|빽다방|컴포즈|커피빈|폴바셋|엔제리너스|" +
            "맥도날드|롯데리아|버거킹|맘스터치|KFC|서브웨이|" +
            "본죽|김밥천국|한솥|명랑|BBQ|BHC|교촌|굽네|네네|처갓집|페리카나|" +
            "파리바게뜨|뚜레쥬르|배스킨|던킨";

    private static final List<String> MEAL_ONLY = List.of("한식", "양식", "일식", "중식");
    private static final List<String> CAFE = List.of("카페");

    @Getter
    public static class CourseStop {
        private final String slot;
        private final StoreListItem store;
        private String distanceLabel;   // 아침 가게 기준 거리 ("+1.2km"), 아침은 "출발"

        public CourseStop(String slot, StoreListItem store) {
            this.slot = slot; this.store = store;
        }
        public void setDistanceLabel(String label) { this.distanceLabel = label; }
    }

    public List<String> getRegions() {
        return storeMapper.findCourseRegions();
    }

    public List<CourseStop> getCourse(String region) {
        List<CourseStop> course = new ArrayList<>();
        List<Long> picked = new ArrayList<>();
        // 날짜 + 지역 기반 seed -> 같은 날 같은 지역이면 항상 같은 코스, 자정 지나면 자동으로 바뀜
        // (RAND(seed)에 음수를 넘기지 않도록 절댓값 사용)
        long seed = Math.abs(LocalDate.now().toEpochDay() * 31 + region.hashCode());

        addStopWithFallback(course, picked, region, "아침", CAFE, seed);
        addStopWithFallback(course, picked, region, "점심", MEAL_ONLY, seed);
        addStopWithFallback(course, picked, region, "저녁", MEAL_ONLY, seed);

        applyDistances(course);
        return course;
    }

    /**
     * 해당 시간대 가게를 후보 중 랜덤으로 뽑는다. 단계적으로 조건을 완화해서
     * 슬롯이 항상 채워지도록 폴백한다:
     *   1) 시간대 카테고리 + 별점 3.0 이상 + 프랜차이즈 제외 → 랜덤
     *   2) (콜드스타트) 별점 조건 없이 시간대 카테고리 + 프랜차이즈 제외 → 랜덤
     *   3) 카테고리 조건도 풀고 그 지역 전체 + 프랜차이즈 제외 → 랜덤
     *   4) 최후: 프랜차이즈 제외도 풀고 그 지역 아무 가게 → 랜덤 (반드시 채움)
     */
    private void addStopWithFallback(List<CourseStop> course, List<Long> picked,
                                     String region, String slot, List<String> categories, long seed) {
        StoreListItem store = storeMapper.findCourseStore(
                region, categories, picked, FRANCHISE_PATTERN, MIN_RATING, seed);
        if (store == null) {
            store = storeMapper.findCourseStore(
                    region, categories, picked, FRANCHISE_PATTERN, null, seed);
        }
        if (store == null) {
            store = storeMapper.findCourseStore(
                    region, null, picked, FRANCHISE_PATTERN, null, seed);
        }
        if (store == null) {
            store = storeMapper.findCourseStore(
                    region, null, picked, null, null, seed);
        }
        if (store != null) {
            picked.add(store.getStoreId());
            course.add(new CourseStop(slot, store));
        }
    }

    /** 첫 스탑(아침)을 기준점으로 각 스탑까지의 거리 라벨 계산 */
    private void applyDistances(List<CourseStop> course) {
        if (course.isEmpty()) return;
        StoreListItem base = course.get(0).getStore();
        for (int i = 0; i < course.size(); i++) {
            CourseStop stop = course.get(i);
            if (i == 0) {
                stop.setDistanceLabel("출발");
                continue;
            }
            Double d = distanceKm(base, stop.getStore());
            stop.setDistanceLabel(d == null ? null : String.format("+%.1fkm", d));
        }
    }

    /** 두 지점 사이 거리(km) — 하버사인 공식 */
    private Double distanceKm(StoreListItem a, StoreListItem b) {
        if (a.getLatitude() == null || a.getLongitude() == null
                || b.getLatitude() == null || b.getLongitude() == null) return null;
        double R = 6371.0;
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
