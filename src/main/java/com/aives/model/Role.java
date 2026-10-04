package com.aives.model;

public enum Role {
    ADMIN("Quản trị viên"),
    LECTURER("Giảng viên"),
    STUDENT("Sinh viên");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
