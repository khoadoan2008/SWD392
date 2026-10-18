package com.aives.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionSource {
    MANUAL("Thủ công"),
    IMPORT("Nhập file"),
    AI("AI sinh");

    private final String label;
}
