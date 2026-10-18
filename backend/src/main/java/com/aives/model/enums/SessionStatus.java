package com.aives.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    IN_PROGRESS("Đang thi"),
    COMPLETED("Hoàn thành"),
    ABORTED("Đã huỷ");

    private final String label;
}
