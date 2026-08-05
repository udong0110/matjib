package com.example.matjib.service;

import com.example.matjib.dto.StoreListItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// 두 가게 사이 도보/대중교통 이동 정보 조회 (카카오맵 경로 조회 API)
// 도보는 API 실패해도 직선거리+속도로 대체해서 항상 값을 주고, 대중교통은 실패하면 그냥 null (화면에서 도보만 표시)
@Slf4j
@Service
public class RouteService {

    private static final String WALK_URL = "https://dapi.kakao.com/v2/routing/walk";
    private static final String TRANSIT_URL = "https://dapi.kakao.com/v2/routing/publictraffic";
    private static final double WALK_SPEED_KMH = 4.0;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kakao.api-key}")
    private String kakaoApiKey;

    public record WalkInfo(double km, int minutes) {}
    public record TransitInfo(int minutes) {}

    public WalkInfo getWalkInfo(StoreListItem from, StoreListItem to) {
        Double straightKm = straightLineKm(from, to);
        if (straightKm == null) {
            return null;
        }
        if (!hasApiKey()) {
            return fallbackWalk(straightKm);
        }
        try {
            JsonNode root = requestRoute(WALK_URL, from, to);
            if (!"OK".equals(root.path("status").asText())) {
                return fallbackWalk(straightKm);
            }
            JsonNode props = root.path("route").path("properties");
            double km = props.path("totalDistance").asInt() / 1000.0;
            int minutes = Math.max(1, (int) Math.round(props.path("totalTime").asInt() / 60.0));
            return new WalkInfo(km, minutes);
        } catch (Exception e) {
            log.warn("도보 경로 조회 실패, 직선거리로 대체: {}", e.getMessage());
            return fallbackWalk(straightKm);
        }
    }

    // 버스/지하철 경로 중 제일 빠른 걸로 소요시간만 반환. 경로 없거나 실패하면 null
    public TransitInfo getTransitInfo(StoreListItem from, StoreListItem to) {
        if (from.getLatitude() == null || from.getLongitude() == null
                || to.getLatitude() == null || to.getLongitude() == null || !hasApiKey()) {
            return null;
        }
        try {
            JsonNode root = requestRoute(TRANSIT_URL, from, to);
            if (!"OK".equals(root.path("status").asText())) {
                return null;
            }
            int fastestSec = Integer.MAX_VALUE;
            for (JsonNode route : root.path("routes")) {
                fastestSec = Math.min(fastestSec, route.path("properties").path("totalTime").asInt(Integer.MAX_VALUE));
            }
            if (fastestSec == Integer.MAX_VALUE) {
                return null;
            }
            return new TransitInfo(Math.max(1, (int) Math.round(fastestSec / 60.0)));
        } catch (Exception e) {
            log.warn("대중교통 경로 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode requestRoute(String url, StoreListItem from, StoreListItem to) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("start_x", from.getLongitude())
                .queryParam("start_y", from.getLatitude())
                .queryParam("end_x", to.getLongitude())
                .queryParam("end_y", to.getLatitude())
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        return objectMapper.readTree(response.getBody());
    }

    private boolean hasApiKey() {
        return kakaoApiKey != null && !kakaoApiKey.isBlank();
    }

    private WalkInfo fallbackWalk(double straightKm) {
        int minutes = Math.max(1, (int) Math.round(straightKm / WALK_SPEED_KMH * 60));
        return new WalkInfo(straightKm, minutes);
    }

    // 하버사인 공식으로 두 지점 사이 직선거리(km) 계산
    private Double straightLineKm(StoreListItem a, StoreListItem b) {
        if (a.getLatitude() == null || a.getLongitude() == null
                || b.getLatitude() == null || b.getLongitude() == null) {
            return null;
        }
        double r = 6371.0;
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
