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
 * 앱 시작 시 카카오 로컬 API로 가게 데이터를 긁어와서 store 테이블에 저장.
 * application.yml의 kakao.seed-enabled=true일 때만 동작함.
 *
 * API는 초기 데이터 채우는 용도로만 쓰고, 실제 조회는 전부 DB에서 MyBatis로 처리한다
 * (과제 요구사항인 Join/동적쿼리를 유지하기 위함).
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

    // 검색 키워드 목록. 지역/메뉴명을 구체적으로 넣을수록 체인점 대신 로컬 가게가 많이 잡힘
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
        // 재시작할 때마다 다시 긁어오지 않게, 데이터 있으면 스킵
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
                // 한 키워드가 실패해도 나머지는 계속 진행
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
