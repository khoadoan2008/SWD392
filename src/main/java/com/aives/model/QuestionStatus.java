package com.aives.model;

/** Vòng đời câu hỏi: chỉ APPROVED mới thuộc ngân hàng chính thức. */
public enum QuestionStatus {
    DRAFT("Nháp"),
    PENDING_REVIEW("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Loại bỏ");

    private final String label;

    QuestionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
