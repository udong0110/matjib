package com.example.matjib.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PointHistory {
    private Long historyId;
    private Long memberId;
    private Integer amount;      // +100(적립) / -3000(환급)
    private String type;         // EARN / REFUND
    private String reason;       // "리뷰 작성" / "포인트 환급"
    private LocalDateTime createdAt;

    @Builder
    public PointHistory(Long memberId, Integer amount, String type, String reason) {
        this.memberId = memberId;
        this.amount = amount;
        this.type = type;
        this.reason = reason;
    }
}
