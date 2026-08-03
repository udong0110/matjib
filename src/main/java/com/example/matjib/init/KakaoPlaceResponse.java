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
        private String placeName;           // 가게 이름

        @JsonProperty("category_name")
        private String categoryName;        // "음식점 > 한식 > 육류,고기" 형태

        @JsonProperty("road_address_name")
        private String roadAddressName;     // 도로명 주소

        @JsonProperty("address_name")
        private String addressName;         // 지번 주소

        private String phone;               // 전화번호
        private String x;                   // 경도(longitude)
        private String y;                   // 위도(latitude)
    }
}
