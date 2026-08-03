package com.example.matjib.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class ErrorResponse {
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String message;
    private final Map<String, String> fieldErrors;   // 유효성 검증 실패 상세 (없으면 null)

    public ErrorResponse(int status, String message, Map<String, String> fieldErrors) {
        this.status = status;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}
