package com.example.matjib.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Visit {
    private Long visitId;
    private Long memberId;
    private Long storeId;
    private LocalDateTime visitedAt;

    @Builder
    public Visit(Long memberId, Long storeId) {
        this.memberId = memberId;
        this.storeId = storeId;
    }
}
