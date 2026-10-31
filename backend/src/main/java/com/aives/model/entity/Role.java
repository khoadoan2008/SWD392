package com.aives.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role implements Serializable {

    public static final String ADMIN_NAME = "ADMIN";
    public static final String LECTURER_NAME = "LECTURER";
    public static final String STUDENT_NAME = "STUDENT";

    public static final Role ADMIN = new Role(1, ADMIN_NAME, "Quản trị hệ thống");
    public static final Role LECTURER = new Role(2, LECTURER_NAME, "Giảng viên");
    public static final Role STUDENT = new Role(3, STUDENT_NAME, "Sinh viên");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(length = 200)
    private String description;

    public Role(String name) {
        this.name = name != null ? name.toUpperCase() : null;
    }

    public Role(Integer id, String name) {
        this.id = id;
        this.name = name != null ? name.toUpperCase() : null;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static Role fromString(String val) {
        return from(val);
    }

    public static Role valueOf(String val) {
        return from(val);
    }

    private static Role from(String val) {
        if (val == null || val.isBlank()) return null;
        String upper = val.trim().toUpperCase();
        if (ADMIN_NAME.equals(upper)) return ADMIN;
        if (LECTURER_NAME.equals(upper)) return LECTURER;
        if (STUDENT_NAME.equals(upper)) return STUDENT;
        return new Role(upper);
    }

    public boolean isAdmin() {
        return ADMIN_NAME.equalsIgnoreCase(name);
    }

    public boolean isLecturer() {
        return LECTURER_NAME.equalsIgnoreCase(name);
    }

    public boolean isStudent() {
        return STUDENT_NAME.equalsIgnoreCase(name);
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof Role other) {
            return Objects.equals(name != null ? name.toUpperCase() : null,
                    other.name != null ? other.name.toUpperCase() : null);
        }
        if (o instanceof String str) {
            return str.equalsIgnoreCase(name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name != null ? name.toUpperCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}

