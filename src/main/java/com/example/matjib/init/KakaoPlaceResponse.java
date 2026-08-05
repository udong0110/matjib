package com.example.matjib.init;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 카카오 로컬 "키워드로 장소 검색" API 응답 매핑
 * GET https://dapi.kakao.com/v2/local/search/keyword.json
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoPlaceResponse {

    private List<Document> documents;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        @JsonProperty("place_name")
        private String placeName;

        @JsonProperty("category_name")
        private String categoryName;        // "음식점 > 한식 > 육류,고기" 형태로 내려옴

        @JsonProperty("road_address_name")
        private String roadAddressName;

        @JsonProperty("address_name")
        private String addressName;

        private String phone;
        private String x;                   // 경도. 카카오는 위경도 아니고 x/y 순으로 줌
        private String y;                   // 위도
    }
}
