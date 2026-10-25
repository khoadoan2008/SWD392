package com.aives.dto.response;

import com.aives.model.entity.Subject;

public record SubjectResponse(
        Integer id,
        String code,
        String name,
        String description,
        boolean active
) {
    public static SubjectResponse of(Subject s) {
        return s == null ? null : new SubjectResponse(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getDescription(),
                s.isActive()
        );
    }
}
