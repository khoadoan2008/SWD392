package com.aives.dto.response;

import com.aives.model.entity.Rubric;

import java.math.BigDecimal;

public record RubricResponse(
        Integer id,
        Integer subjectId,
        String criterion,
        String keywords,
        BigDecimal maxScore,
        Integer sortOrder
) {
    public static RubricResponse of(Rubric r) {
        if (r == null) return null;
        Integer sId = r.getSubject() != null ? r.getSubject().getId() : null;
        return new RubricResponse(
                r.getId(),
                sId,
                r.getCriterion(),
                r.getKeywords(),
                r.getMaxScore(),
                r.getSortOrder()
        );
    }
}
