package com.aives.exception;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ của luồng chấm điểm (AI gợi ý + giảng viên chốt điểm), mang sẵn mã HTTP trả về. */
public class GradingException extends RuntimeException {

    private final HttpStatus status;

    public GradingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
