package com.example.matjib.service;

import com.example.matjib.dto.StoreListItem;
import com.example.matjib.mapper.StoreMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 지역별 맛집 코스 추천.
 * 아침(카페) / 점심(식사류+카페) / 저녁(식사류, 카페 제외) 각 1곳씩.
 * 선정 우선순위: 리뷰10개+별점순 → 리뷰많은순 → 로컬(프랜차이즈 제외) 우선.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final StoreMapper storeMapper;

    // 프랜차이즈(체인) 추정 이름 패턴 — 이 이름이 들어가면 로컬이 아니라고 보고 뒤로 밀기
    private static final String FRANCHISE_PATTERN =
            "스타벅스|투썸|이디야|메가|빽다방|컴포즈|커피빈|폴바셋|엔제리너스|" +
            "맥도날드|롯데리아|버거킹|맘스터치|KFC|서브웨이|" +
            "본죽|김밥천국|한솥|명랑|BBQ|BHC|교촌|굽네|네네|처갓집|페리카나|" +
            "파리바게뜨|뚜레쥬르|배스킨|던킨";

    private static final List<String> MEAL_ONLY = List.of("한식", "양식", "일식", "중식");
    private static final List<String> CAFE = List.of("카페");
    private static final List<String> LUNCH = List.of("한식", "양식", "일식", "중식", "카페");

    @Getter
    public static class CourseStop {
        private final String slot;      // 아침/점심/저녁
        private final String emoji;
        private final StoreListItem store;
        private String distanceLabel;   // 아침 가게 기준 거리 ("+1.2km"), 아침은 "출발"

        public CourseStop(String slot, String emoji, StoreListItem store) {
            this.slot = slot; this.emoji = emoji; this.store = store;
        }
        public void setDistanceLabel(String label) { this.distanceLabel = label; }
    }

    public List<String> getRegions() {
        return storeMapper.findCourseRegions();
    }

    public List<CourseStop> getCourse(String region) {
        List<CourseStop> course = new ArrayList<>();
        List<Long> picked = new ArrayList<>();

        // 🌅 아침 = 카페 → 없으면 일반 식당 → 그래도 없으면 아무 가게
        addStopWithFallback(course, picked, region, "아침", "🌅", CAFE);
        // ☀️ 점심 = 일반 식당+카페 → 없으면 아무 가게
        addStopWithFallback(course, picked, region, "점심", "☀️", LUNCH);
        // 🌙 저녁 = 일반 식당 → 없으면 아무 가게
        addStopWithFallback(course, picked, region, "저녁", "🌙", MEAL_ONLY);

        applyDistances(course);
        return course;
    }

    /**
     * 해당 시간대 가게를 뽑되, 조건에 맞는 가게가 없으면
     * 카테고리 조건을 풀고 그 지역 아무 가게라도 끌어와서 슬롯을 반드시 채운다.
     */
    private void addStopWithFallback(List<CourseStop> course, List<Long> picked,
                                     String region, String slot, String emoji, List<String> categories) {
        StoreListItem store = storeMapper.findCourseStore(region, categories, picked, FRANCHISE_PATTERN);
        if (store == null) {
            // 카테고리 조건 없이 그 지역 아무 가게 (리뷰 많은 순) — 강제 채움
            store = storeMapper.findCourseStore(region, null, picked, FRANCHISE_PATTERN);
        }
        if (store != null) {
            picked.add(store.getStoreId());
            course.add(new CourseStop(slot, emoji, store));
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

    // 특정 가게가 해당 지역 코스에 포함되는지 (포인트 2배 판단용)
    public boolean isCourseStore(Long storeId, String region) {
        return getCourse(region).stream()
                .anyMatch(stop -> stop.getStore().getStoreId().equals(storeId));
    }
}
