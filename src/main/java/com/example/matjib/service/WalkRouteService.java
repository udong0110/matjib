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

// 두 가게 사이 도보 거리/시간 조회 (카카오맵 도보 경로 조회 API)
// API 실패하거나 키가 없으면 직선거리 + 도보 속도(4km/h)로 대충 계산해서 대체
@Slf4j
@Service
public class WalkRouteService {

    private static final String WALK_URL = "https://dapi.kakao.com/v2/routing/walk";
    private static final double WALK_SPEED_KMH = 4.0;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kakao.api-key}")
    private String kakaoApiKey;

    public record WalkInfo(double km, int minutes) {}

    public WalkInfo getWalkInfo(StoreListItem from, StoreListItem to) {
        Double straightKm = straightLineKm(from, to);
        if (straightKm == null) {
            return null;
        }
        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            return fallback(straightKm);
        }
        try {
            return callWalkApi(from, to, straightKm);
        } catch (Exception e) {
            log.warn("도보 경로 조회 실패, 직선거리로 대체: {}", e.getMessage());
            return fallback(straightKm);
        }
    }

    private WalkInfo callWalkApi(StoreListItem from, StoreListItem to, double straightKm) {
        URI uri = UriComponentsBuilder.fromHttpUrl(WALK_URL)
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
        JsonNode root = readTree(response.getBody());
        if (!"OK".equals(root.path("status").asText())) {
            return fallback(straightKm);
        }
        JsonNode props = root.path("route").path("properties");
        double km = props.path("totalDistance").asInt() / 1000.0;
        int minutes = Math.max(1, (int) Math.round(props.path("totalTime").asInt() / 60.0));
        return new WalkInfo(km, minutes);
    }

    private JsonNode readTree(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("도보 경로 응답 파싱 실패", e);
        }
    }

    private WalkInfo fallback(double straightKm) {
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
