package com.example.matjib.init;

import com.example.matjib.domain.Store;
import com.example.matjib.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * 앱 시작 시 카카오 로컬 API로 실제 가게 데이터를 수집해 store 테이블에 저장한다.
 * application.yml 의 kakao.seed-enabled=true 일 때만 동작.
 *
 * ★ 핵심: API는 "초기 데이터 채우기"용으로만 쓰고,
 *   실제 서비스 조회는 전부 우리 DB(store 테이블)에서 MyBatis로 수행한다.
 *   → 과제 요구사항(Join + 동적쿼리)이 그대로 유지됨.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreDataSeeder implements ApplicationRunner {

    private final StoreMapper storeMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.api-key}")
    private String kakaoApiKey;

    @Value("${kakao.seed-enabled}")
    private boolean seedEnabled;

    private static final String KAKAO_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    // 수집할 검색 키워드 (부산 지역별 + 로컬 맛집 위주)
    // 구체적인 지역/메뉴 키워드를 많이 넣을수록 체인점보다 로컬 가게가 많이 수집됨
    private static final List<String> KEYWORDS = List.of(
            // 지역 대표 맛집
            "해운대 맛집", "서면 맛집", "광안리 맛집", "남포동 맛집", "전포동 맛집",
            "부산대 맛집", "기장 맛집", "송정 맛집", "센텀시티 맛집", "동래 맛집",
            // 부산 로컬 대표 메뉴 (찐 로컬맛집 위주)
            "부산 돼지국밥", "부산 밀면", "부산 회", "부산 곰장어", "부산 씨앗호떡",
            "부산 어묵", "부산 낙곱새", "부산 돼지갈비", "부산 활어회", "부산 물떡",
            "부산 복국", "부산 대구탕", "부산 아구찜", "수영구 복국", "남포동 복국",
            // 카페/디저트
            "광안리 카페", "전포 카페", "해운대 카페", "영도 카페"
    );

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            log.info("[StoreSeeder] kakao.seed-enabled=false → API 시딩 건너뜀");
            return;
        }
        // 이미 가게 데이터가 충분히 있으면 재수집하지 않음 (재시작 시 중복 호출 방지)
        if (storeMapper.findAll().size() >= 20) {
            log.info("[StoreSeeder] 가게 데이터가 이미 있어 시딩 건너뜀 ({}건)", storeMapper.findAll().size());
            return;
        }
        log.info("[StoreSeeder] 카카오 API로 부산 가게 데이터 수집 시작");
        int saved = 0;
        for (String keyword : KEYWORDS) {
            try {
                saved += fetchAndSave(keyword);
            } catch (Exception e) {
                // API 실패해도 앱은 계속 뜬다 (한 키워드 실패가 전체를 막지 않도록)
                log.warn("[StoreSeeder] '{}' 수집 실패: {}", keyword, e.getMessage());
            }
        }
        log.info("[StoreSeeder] 완료 - 신규 저장 {}건", saved);
    }

    private int fetchAndSave(String keyword) {
        URI uri = UriComponentsBuilder.fromHttpUrl(KAKAO_URL)
                .queryParam("query", keyword)
                .queryParam("size", 15)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<KakaoPlaceResponse> response =
                restTemplate.exchange(uri, HttpMethod.GET, entity, KakaoPlaceResponse.class);

        KakaoPlaceResponse body = response.getBody();
        if (body == null || body.getDocuments() == null) {
            return 0;
        }

        int count = 0;
        for (KakaoPlaceResponse.Document doc : body.getDocuments()) {
            Store store = toStore(doc);
            // 부산 지역이 아니면 건너뜀 (타 지역 데이터 방지)
            if (store.getRegion() == null || !store.getRegion().startsWith("부산")) {
                continue;
            }
            // 중복(같은 이름+주소) 방지
            if (storeMapper.countByNameAndAddress(store) == 0) {
                storeMapper.insert(store);
                count++;
            }
        }
        log.info("[StoreSeeder] '{}' → {}건 저장", keyword, count);
        return count;
    }

    private Store toStore(KakaoPlaceResponse.Document doc) {
        String address = (doc.getRoadAddressName() != null && !doc.getRoadAddressName().isBlank())
                ? doc.getRoadAddressName() : doc.getAddressName();
        Double lat = parseDouble(doc.getY());
        Double lng = parseDouble(doc.getX());
        return Store.builder()
                .storeName(doc.getPlaceName())
                .category(extractCategory(doc.getCategoryName()))
                .region(extractRegion(address))
                .address(address)
                .phone(doc.getPhone())
                .latitude(lat)
                .longitude(lng)
                .build();
    }

    private Double parseDouble(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // "음식점 > 한식 > 육류,고기" → "한식"
    private String extractCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return "기타";
        String[] parts = categoryName.split(">");
        return parts.length >= 2 ? parts[1].trim() : parts[0].trim();
    }

    // "부산 해운대구 중동 ..." → "부산 해운대구"
    private String extractRegion(String address) {
        if (address == null || address.isBlank()) return "미상";
        String[] parts = address.trim().split(" ");
        return parts.length >= 2 ? parts[0] + " " + parts[1] : parts[0];
    }
}
