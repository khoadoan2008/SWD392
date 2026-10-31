package com.aives.dto.response;

import com.aives.model.entity.SessionRubricScore;

import java.math.BigDecimal;

public record SessionRubricScoreResponse(
        Integer id,
        Integer rubricId,
        String criterion,
        String keywords,
        BigDecimal maxScore,
        BigDecimal aiScore,
        BigDecimal finalScore,
        String comment
) {
    public static SessionRubricScoreResponse of(SessionRubricScore s) {
        if (s == null) return null;
        return new SessionRubricScoreResponse(
                s.getId(),
                s.getRubric() != null ? s.getRubric().getId() : null,
                s.getRubric() != null ? s.getRubric().getCriterion() : null,
                s.getRubric() != null ? s.getRubric().getKeywords() : null,
                s.getRubric() != null ? s.getRubric().getMaxScore() : null,
                s.getAiScore(),
                s.getFinalScore(),
                s.getComment()
        );
    }
}
