package com.aives.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BloomLevel {
    REMEMBER("Nhớ"),
    UNDERSTAND("Hiểu"),
    APPLY("Vận dụng"),
    ANALYZE("Phân tích"),
    EVALUATE("Đánh giá"),
    CREATE("Sáng tạo");

    private final String label;
}
