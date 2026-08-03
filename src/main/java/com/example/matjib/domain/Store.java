package com.example.matjib.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Store {
    private Long storeId;
    private String storeName;
    private String category;
    private String region;
    private String address;
    private String phone;
    private Double latitude;
    private Double longitude;
    private String imagePath;
    private LocalDateTime createdAt;

    @Builder
    public Store(String storeName, String category, String region, String address,
                 String phone, Double latitude, Double longitude) {
        this.storeName = storeName;
        this.category = category;
        this.region = region;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
