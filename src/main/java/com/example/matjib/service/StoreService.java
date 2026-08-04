package com.example.matjib.service;

import com.example.matjib.domain.Store;
import com.example.matjib.dto.PageResponse;
import com.example.matjib.dto.StoreListItem;
import com.example.matjib.dto.StoreSearch;
import com.example.matjib.exception.BusinessException;
import com.example.matjib.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreMapper storeMapper;

    public List<Store> getAllStores() {
        return storeMapper.findAll();
    }

    public Store getStore(Long storeId) {
        Store store = storeMapper.findById(storeId);
        if (store == null) {
            throw BusinessException.notFound("가게를 찾을 수 없습니다. id=" + storeId);
        }
        return store;
    }

    // 가게 목록 (이름 검색 + 지역 필터 + 페이징)
    public PageResponse<StoreListItem> getStores(StoreSearch search) {
        List<StoreListItem> content = storeMapper.findStores(search);
        long total = storeMapper.countStores(search);
        return new PageResponse<>(content, search.getPage(), search.getSize(), total);
    }

    // 지역 맛집 BEST 3곳 (별점 4.0 이상 중 리뷰 많은 순)
    public List<StoreListItem> getBestStores(String region) {
        return storeMapper.findBestStores(region, 4.0, 3);
    }

    // 가게 대표사진 등록 (관리자)
    public void updateImage(Long storeId, String imagePath) {
        storeMapper.updateImage(storeId, imagePath);
    }
}
