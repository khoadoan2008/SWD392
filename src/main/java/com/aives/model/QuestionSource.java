package com.aives.model;

public enum QuestionSource {
    MANUAL("Nhập tay"),
    IMPORT("Import"),
    AI("AI sinh");

    private final String label;

    QuestionSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
