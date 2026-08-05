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
 * 지역별 코스 추천 (아침/점심은 카페+식사류 중 랜덤 1곳, 저녁은 식사류만 1곳).
 * 맨날 같은 맛집만 뜨지 않도록 별점 3점대 후보 중 랜덤으로 뽑고, 프랜차이즈는 최대한 제외한다.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final StoreMapper storeMapper;
    private final WalkRouteService walkRouteService;

    private static final double MIN_RATING = 3.0;

    private static final List<String> MEAL_ONLY = List.of("한식", "양식", "일식", "중식");
    private static final List<String> CAFE_OR_MEAL = List.of("카페", "한식", "양식", "일식", "중식");

    @Getter
    public static class CourseStop {
        private final String slot;
        private final StoreListItem store;
        private String distanceLabel;   // "1.2km · 도보 18분" 식으로 표시, 아침은 "출발"

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
        // 날짜+지역으로 시드를 고정해서 같은 날 같은 지역이면 코스가 안 바뀌게 함 (자정 지나면 바뀜)
        // hashCode가 음수일 수 있어서 abs 처리
        long seed = Math.abs(LocalDate.now().toEpochDay() * 31 + region.hashCode());

        addStopWithFallback(course, picked, region, "아침", CAFE_OR_MEAL, seed);
        addStopWithFallback(course, picked, region, "점심", CAFE_OR_MEAL, seed);
        addStopWithFallback(course, picked, region, "저녁", MEAL_ONLY, seed);

        applyDistances(course);
        return course;
    }

    /**
     * 후보가 안 잡히면 아래 순서로 조건을 하나씩 풀면서 다시 찾는다 (슬롯은 무조건 채운다).
     *   1. 카테고리 + 별점 3.0 이상 + 프랜차이즈 제외
     *   2. 별점 조건 빼고
     *   3. 카테고리도 빼고 지역 전체에서
     *   4. 마지막엔 프랜차이즈 조건까지 빼고 아무 가게나
     */
    private void addStopWithFallback(List<CourseStop> course, List<Long> picked,
                                     String region, String slot, List<String> categories, long seed) {
        StoreListItem store = storeMapper.findCourseStore(
                region, categories, picked, StoreMapper.FRANCHISE_PATTERN, MIN_RATING, seed);
        if (store == null) {
            store = storeMapper.findCourseStore(
                    region, categories, picked, StoreMapper.FRANCHISE_PATTERN, null, seed);
        }
        if (store == null) {
            store = storeMapper.findCourseStore(
                    region, null, picked, StoreMapper.FRANCHISE_PATTERN, null, seed);
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

    // 앞 스탑 → 다음 스탑 구간별로 도보 거리/시간 계산 (아침은 시작점이라 "출발"만 표시)
    private void applyDistances(List<CourseStop> course) {
        for (int i = 0; i < course.size(); i++) {
            CourseStop stop = course.get(i);
            if (i == 0) {
                stop.setDistanceLabel("출발");
                continue;
            }
            StoreListItem prev = course.get(i - 1).getStore();
            WalkRouteService.WalkInfo info = walkRouteService.getWalkInfo(prev, stop.getStore());
            stop.setDistanceLabel(info == null ? null
                    : String.format("%.1fkm · 도보 %d분", info.km(), info.minutes()));
        }
    }
}
