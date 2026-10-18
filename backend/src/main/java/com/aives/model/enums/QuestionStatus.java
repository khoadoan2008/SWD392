package com.aives.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionStatus {
    DRAFT("Bản nháp"),
    PENDING_REVIEW("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Loại bỏ");

    private final String label;
}
