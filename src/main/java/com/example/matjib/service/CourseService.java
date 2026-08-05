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
 * 지역별 코스 추천 (아침 카페 + 점심/저녁 식사 각 1곳).
 * 맨날 같은 맛집만 뜨지 않도록 별점 3점대 후보 중 랜덤으로 뽑고, 프랜차이즈는 최대한 제외한다.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final StoreMapper storeMapper;

    private static final double MIN_RATING = 3.0;

    private static final List<String> MEAL_ONLY = List.of("한식", "양식", "일식", "중식");
    private static final List<String> CAFE = List.of("카페");

    @Getter
    public static class CourseStop {
        private final String slot;
        private final StoreListItem store;
        private String distanceLabel;   // "+1.2km" 식으로 표시, 아침은 "출발"

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

        addStopWithFallback(course, picked, region, "아침", CAFE, seed);
        addStopWithFallback(course, picked, region, "점심", MEAL_ONLY, seed);
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

    /** 아침 가게 기준으로 나머지 스탑 거리 계산 */
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

    /** 하버사인 공식으로 두 지점 사이 거리(km) 계산 */
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
