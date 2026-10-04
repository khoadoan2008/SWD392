package com.aives.model;

/** Thang nhận thức Bloom. */
public enum BloomLevel {
    REMEMBER("Nhớ"),
    UNDERSTAND("Hiểu"),
    APPLY("Vận dụng"),
    ANALYZE("Phân tích"),
    EVALUATE("Đánh giá"),
    CREATE("Sáng tạo");

    private final String label;

    BloomLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
