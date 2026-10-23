package com.aives.dto.response;

import com.aives.model.entity.Subject;

public record GenerationSubjectResponse(Integer id, String code, String name) {
    public static GenerationSubjectResponse of(Subject subject) {
        return subject == null ? null : new GenerationSubjectResponse(subject.getId(), subject.getCode(), subject.getName());
    }
}
